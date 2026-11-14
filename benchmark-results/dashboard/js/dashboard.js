(() => {
"use strict";
const simulation = new URLSearchParams(location.search).get("simulation") || "HelloWorldSimulation";
const dataPath = `data/${encodeURIComponent(simulation)}.json`;
const colors = ["#147d58","#3976c6","#c47736","#8a64b5","#c45b68","#269c9c","#74833b"];
let data, selected, chart, chart2;
const $ = id => document.getElementById(id);
const esc = v => String(v ?? "").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const n = (v,d=0) => v==null || v==="" || !Number.isFinite(Number(v)) ? "—" : new Intl.NumberFormat("en-US",{maximumFractionDigits:d,minimumFractionDigits:d}).format(Number(v));
const compact = v => v==null ? "—" : new Intl.NumberFormat("en-US",{notation:"compact",maximumFractionDigits:2}).format(Number(v));
const ms = v => v==null ? "—" : `${n(v,2)} ms`;
const servers = () => Object.entries(data.servers).map(([id,s],i)=>({id,...s,color:colors[i%colors.length]}));
const rate = m => Number(m.total)>0 ? `${n(Number(m.ok)/Number(m.total)*100,2)}%` : "—";
function header(list){
 $("title").textContent=data.simulation||simulation;
 $("subtitle").textContent=`${list.length} server${list.length===1?"":"s"} · ${list.map(s=>s.name||s.id).join(" / ")}`;
 $("gatling").textContent=[...new Set(list.map(s=>s.gatling?.version).filter(Boolean))].join(", ")||"—";
 $("count").textContent=list.length; $("schema").textContent=data.schemaVersion??"—";
 $("generated").textContent=data.generatedAt||"—"; $("footer-simulation").textContent=data.simulation||simulation;
}
function cards(list){
 $("cards").innerHTML=list.map(s=>`<article class="card ${s.id===selected?"selected":""}" data-id="${esc(s.id)}" style="--c:${s.color}" tabindex="0">
 <div class="card-top"><span class="card-name">${esc(s.name||s.id)}</span><span class="version">${esc(s.gatling?.version||s.id)}</span></div>
 <div class="rps">${n(s.metrics?.meanRps)}</div><div class="muted">mean requests / second</div>
 <div class="card-bottom"><span>p99 <b>${ms(s.metrics?.p99)}</b></span><span>success <b>${rate(s.metrics||{})}</b></span></div></article>`).join("");
 document.querySelectorAll(".card").forEach(el=>{const f=()=>toggleServer(el);el.onclick=f;el.onkeydown=e=>{if(e.key==="Enter"||e.key===" "){e.preventDefault();f();}}});
}
function comparison(list){
 $("comparison").innerHTML=list.map(s=>{const m=s.metrics||{}, samples=s.heapUsage||[], last=samples[samples.length-1]||[], heaps=samples.map(p=>Number(p[1])).filter(Number.isFinite), maxHeap=heaps.length?Math.max(...heaps):null, avgHeap=heaps.length?heaps.reduce((sum,value)=>sum+value,0)/heaps.length:null;return `<tr><td><span class="dot" style="--c:${s.color}"></span><span class="server-name">${esc(s.name||s.id)}</span></td>
 <td>${n(m.meanRps)}</td><td>${ms(m.mean)}</td><td>${ms(m.p50)}</td><td>${ms(m.p95)}</td><td>${ms(m.p99)}</td><td>${ms(m.max)}</td><td><span class="success">${rate(m)}</span></td><td>${compact(m.total)}</td><td>${compact(last[2])}</td><td>${ms(last[3])}</td><td>${maxHeap==null?"—":`${n(maxHeap/(1024*1024),2)} MB`}</td><td>${avgHeap==null?"—":`${n(avgHeap/(1024*1024),2)} MB`}</td></tr>`}).join("");
}
function drawChart(list){
 if(!window.Highcharts){$("chart").innerHTML='<div class="loading">Highcharts failed to load. Check your internet connection.</div>';return;}
 Highcharts.setOptions({lang:{thousandsSep:","},credits:{enabled:false}});
 chart=Highcharts.chart("chart",{chart:{type:"line",backgroundColor: '#ffffff', zooming:{type:"x"},spacing:[12,8,10,8],animation:false,style:{fontFamily:"Inter, sans-serif"}},
 title:{text:null},xAxis:{min:0,title:{text:"Elapsed time (seconds)"},gridLineWidth:0,lineColor:"#e5ebe7",labels:{style:{color:"#87938c"}}},
 yAxis:[
  {min:0,title:{text:"Requests / second"},gridLineColor:"#edf1ee",gridLineDashStyle:"ShortDash",labels:{formatter(){return n(this.value);}}},
  {min:0,title:{text:"Concurrent users"},opposite:true,gridLineWidth:0,labels:{formatter(){return n(this.value);}}}
 ],
 legend:{align:"left",verticalAlign:"bottom",itemStyle:{fontSize:"11px",fontWeight:"600",color:"#526159"},symbolRadius:0,itemHoverStyle:{color:"#147d58"}},
 tooltip:{
  shared:false,
  headerFormat:'<span style="color:#74817a">Elapsed {point.x:.1f}s</span><br>',
  pointFormatter:function(){
   const unit=this.series.yAxis.options.title.text==="Concurrent users"?" users":" req/s";
   return `<span style="color:${this.color}">●</span> <b>${this.series.name}</b><br><b>${n(this.y)}</b>${unit}`;
  }
 },
 plotOptions:{
  series:{animation:false,marker:{enabled:false,states:{hover:{enabled:true,radius:4}}},lineWidth:2.5,states:{inactive:{opacity:.2}},
   events:{
    legendItemClick:function(){
     const group=this.chart.series.filter(
         s=> s.options.serverId===this.options.serverId && s.options.sType==='concurrency'
     );
     group.forEach(s=>s.setVisible(this.visible, false));
     this.chart.redraw();
     return false;
    }
   }}},
 series:list.flatMap(s=>[
   {name:s.name||s.id, serverId: s.id, sType:'throughput', color:s.color,yAxis:0,data:(s.throughput||[]).map(p=>[Number(p[0]),Number(p[1])])},
   {name:`${s.name||s.id} · Users`, serverId: s.id, sType:'concurrency', color:s.color,dashStyle:"ShortDash",yAxis:1,showInLegend:false,data:(s.concurrency||[]).map(p=>[Number(p[0]),Number(p[1])])}
 ]),
 responsive:{rules:[{condition:{maxWidth:600},chartOptions:{legend:{itemStyle:{fontSize:"10px"}}}}]}});
 $("reset").onclick=()=>chart&&chart.zoomOut();
}
function drawChart2(list){
 if(!window.Highcharts){$("chart2").innerHTML='<div class="loading">Highcharts failed to load. Check your internet connection.</div>';return;}
 Highcharts.setOptions({lang:{thousandsSep:","},credits:{enabled:false}});
 chart2=Highcharts.chart("chart2",{chart:{type:"line",backgroundColor:"#ffffff",zooming:{type:"x"},spacing:[12,8,10,8],animation:false,style:{fontFamily:"Inter, sans-serif"}},
 title:{text:null},xAxis:{min:0,title:{text:"Elapsed time (seconds)"},gridLineWidth:0,lineColor:"#e5ebe7",labels:{style:{color:"#87938c"}}},
 yAxis:{min:0,title:{text:"Used heap (MB)"},gridLineColor:"#edf1ee",gridLineDashStyle:"ShortDash",labels:{formatter(){return n(this.value);}},plotLines:[{value:512,color:"#c45b68",dashStyle:"ShortDash",width:2,zIndex:5,label:{text:"Max heap · 512 MB",align:"right",x:-8,style:{color:"#c45b68",fontWeight:"600"}}}]},
 legend:{align:"left",verticalAlign:"bottom",itemStyle:{fontSize:"11px",fontWeight:"600",color:"#526159"},symbolRadius:0,itemHoverStyle:{color:"#147d58"}},
 tooltip:{shared:false,headerFormat:'<span style="color:#74817a">Elapsed {point.x:.1f}s</span><br>',pointFormatter:function(){return `<span style="color:${this.color}">●</span> <b>${this.series.name}</b><br><b>${n(this.y,2)}</b> MB`; }},
 plotOptions:{series:{animation:false,marker:{enabled:false,states:{hover:{enabled:true,radius:4}}},lineWidth:2.5,states:{inactive:{opacity:.2}}}},
 series:list.map(s=>({name:s.name||s.id,serverId:s.id,color:s.color,sType:"heapUsage",data:(s.heapUsage||[]).map(p=>[Number(p[0]),Number(p[1])/(1024*1024)])})),
 responsive:{rules:[{condition:{maxWidth:600},chartOptions:{legend:{itemStyle:{fontSize:"10px"}}}}]}});
 $("reset2").onclick=()=>chart2&&chart2.zoomOut();
}
function detail(s){
 const m=s.metrics||{}, run=s.run||{};
 const env = s.environment||{executionModel:{}};
 const executionModel= env.executionModel||{};
 const executionEntries=Object.entries(executionModel);
 const tiles=[["Total requests",compact(m.total)],["Successful",compact(m.ok)],["Failed",compact(m.ko)],["Mean throughput",`${n(m.meanRps)} req/s`],
 ["Minimum latency",ms(m.min)],["Mean latency",ms(m.mean)],["Median · p50",ms(m.p50)],["p75 latency",ms(m.p75)],
 ["p95 latency",ms(m.p95)],["p99 latency",ms(m.p99)],["Maximum latency",ms(m.max)],["Std. deviation",ms(m.stdDev)]];
 const req=Object.entries(s.requests||{});
 $("detail").innerHTML=`<div class="detail-head"><div><h3>${esc(s.name||s.id)}</h3><p>${esc(s.requestName||"All requests")} · ${esc(run.date||"Run date unavailable")}</p></div>
 <div class="badges"><span class="badge">Server ID: ${esc(s.id)}</span><span class="badge">Gatling ${esc(s.gatling?.version||"—")}</span>${run.duration?`<span class="badge">Duration: ${esc(run.duration)}</span>`:""}</div></div>
 <div class="execution-model">
  <div class="execution-grid">
    ${executionEntries.map(([key,value])=>`
      <div class="execution-item">
        <span>${esc(key)}</span>
        <b>${esc(value)}</b>
      </div>
    `).join("")||'<div class="muted">No execution model configuration available.</div>'}
  </div>
 </div>
 <div class="metric-grid">${tiles.map(([a,b])=>`<div class="metric"><span>${a}</span><b>${b}</b></div>`).join("")}</div>
 <div class="request-head"><b>Request breakdown</b><span>${req.length} request${req.length===1?"":"s"}</span></div>
 <div class="scroll"><table class="request-table"><thead><tr><th>REQUEST</th><th>TOTAL</th><th>OK</th><th>KO</th><th>MEAN</th><th>P50</th><th>P95</th><th>P99</th></tr></thead><tbody>
 ${req.map(([id,r])=>`<tr><td>${esc(r.name||id)}</td><td>${compact(r.total)}</td><td>${compact(r.ok)}</td><td>${compact(r.ko)}</td><td>${ms(r.mean)}</td><td>${ms(r.p50)}</td><td>${ms(r.p95)}</td><td>${ms(r.p99)}</td></tr>`).join("")||'<tr><td colspan="8">No request-level statistics available.</td></tr>'}
 </tbody></table></div>`;
}
function tabs(list){$("tabs").innerHTML=list.map(s=>`<button class="tab" role="tab" data-id="${esc(s.id)}" aria-selected="${s.id===selected}">${esc(s.name||s.id)}</button>`).join("");document.querySelectorAll(".tab").forEach(t=>t.onclick=()=>select(t.dataset.id));}
function select(id){selected=id;const list=servers(),s=list.find(x=>x.id===id);if(!s)return;cards(list);tabs(list);detail(s);}
function toggleServer(el){
  const id = el.dataset.id;
  const selected = el.classList.contains("selected");
  if (!selected) {
    select(id)
    document.querySelector(`.card:not([data-id="${id}"])`).classList.add("disabled") // disable all others
    chart.series.forEach(s=>s.setVisible(false, false)); // hide all
    chart2.series.forEach(s=>s.setVisible(false, false));
    const group= chart.series.filter(s=> s.options.serverId===id);
    group.forEach(s=>s.setVisible(true, false)); // select server
    chart2.series.filter(s=>s.options.serverId===id).forEach(s=>s.setVisible(true, false));
  } else {
    document.querySelector(`.card:not([data-id="${id}"])`).classList.remove("disabled") // enable all others
    el.classList.remove("selected") // deselect
    chart.series.forEach(s=>s.setVisible(true, false)); // show all
    chart2.series.forEach(s=>s.setVisible(true, false));
  }
  chart.redraw();
  chart2.redraw();
}
function fail(e){document.querySelector("main").hidden=true;document.querySelector(".topbar").hidden=true;$("error").hidden=false;$("error-message").textContent=e.message||String(e);$("error-path").textContent=dataPath;}
async function init(){try{const r=await fetch(dataPath,{cache:"no-store"});if(!r.ok)throw Error(`HTTP ${r.status}: ${dataPath}`);data=await r.json();if(!data.servers||!Object.keys(data.servers).length)throw Error("No server results found in the JSON file.");const list=servers();header(list);cards(list);comparison(list);drawChart(list);drawChart2(list);tabs(list);detail(list[0]);}catch(e){fail(e);}}
document.addEventListener("DOMContentLoaded",init);
})();
