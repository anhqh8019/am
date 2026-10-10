import { useEffect, useMemo, useState } from "react";
import { Banknote, Download, FileSpreadsheet, WalletCards } from "lucide-react";
import { api } from "./api";

type Building={id:number;code:string;name:string};
type Period={id:number;code:string};
type Item={description:string;quantity:number;unitPrice:number;amount:number;paidAmount:number;outstandingAmount:number};
type Bill={sourceType:string;referenceCode:string;periodCode:string;buildingId:number;buildingName:string;unitCode:string;customerName:string;dueDate:string;totalAmount:number;paidAmount:number;outstandingAmount:number;status:string;items:Item[]};
type Payment={paymentId:number;periodCode:string;sourceType:string;buildingId:number;buildingName:string;locationCode:string;customerName:string;receivableReference:string;paymentDate:string;paymentMethod:string;transactionReference?:string;note?:string;serviceDescription:string;amount:number};

const money=(n:number)=>new Intl.NumberFormat("vi-VN").format(n)+" đ";
const esc=(v:unknown)=>String(v??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
const cell=(value:unknown,type="String")=>`<Cell><Data ss:Type="${type}">${esc(value)}</Data></Cell>`;
const row=(values:{value:unknown;type?:string}[])=>`<Row>${values.map(v=>cell(v.value,v.type)).join("")}</Row>`;
const methodLabel=(v:string)=>({CASH:"Tiền mặt",BANK_TRANSFER:"Chuyển khoản",QR:"QR ngân hàng"}[v]??v);

export function ReportsPage(){
 const [periods,setPeriods]=useState<Period[]>([]),[buildings,setBuildings]=useState<Building[]>([]),[bills,setBills]=useState<Bill[]>([]),[payments,setPayments]=useState<Payment[]>([]);
 const [periodId,setPeriodId]=useState(""),[buildingId,setBuildingId]=useState(""),[source,setSource]=useState(""),[loading,setLoading]=useState(true),[error,setError]=useState("");
 async function load(selected:string){setLoading(true);try{const suffix=selected?`?periodId=${selected}`:"";const [r,p]=await Promise.all([api<Bill[]>(`/billing/receivables${suffix}`),api<Payment[]>(`/reports/payments${suffix}`)]);setBills(r);setPayments(p);setError("");}catch(e){setError(e instanceof Error?e.message:"Không tải được báo cáo.");}finally{setLoading(false)}}
 useEffect(()=>{Promise.all([api<Period[]>("/billing/periods"),api<Building[]>("/buildings")]).then(([p,b])=>{setPeriods(p);setBuildings(b);const id=p.length?String(p[0].id):"";setPeriodId(id);load(id)}).catch(e=>{setError(e.message);setLoading(false)})},[]);
 const filteredBills=useMemo(()=>bills.filter(b=>(!buildingId||b.buildingId===Number(buildingId))&&(!source||b.sourceType===source)),[bills,buildingId,source]);
 const filteredPayments=useMemo(()=>payments.filter(p=>(!buildingId||p.buildingId===Number(buildingId))&&(!source||p.sourceType===source)),[payments,buildingId,source]);
 const summary=useMemo(()=>({due:filteredBills.reduce((s,b)=>s+b.totalAmount,0),paid:filteredBills.reduce((s,b)=>s+b.paidAmount,0),outstanding:filteredBills.reduce((s,b)=>s+b.outstandingAmount,0),received:filteredPayments.reduce((s,p)=>s+p.amount,0)}),[filteredBills,filteredPayments]);
 function exportExcel(){
  const debtRows=filteredBills.flatMap(b=>b.items.map(i=>row([{value:b.periodCode},{value:b.buildingName},{value:b.sourceType==="COMMERCIAL"?"Mặt bằng":"Căn hộ"},{value:b.unitCode},{value:b.customerName},{value:b.referenceCode},{value:i.description},{value:i.quantity,type:"Number"},{value:i.unitPrice,type:"Number"},{value:i.amount,type:"Number"},{value:i.paidAmount,type:"Number"},{value:i.outstandingAmount,type:"Number"},{value:b.dueDate},{value:b.status}]))).join("");
  const paymentRows=filteredPayments.map(p=>row([{value:p.paymentDate.replace("T"," ").slice(0,19)},{value:p.periodCode},{value:p.buildingName},{value:p.sourceType==="COMMERCIAL"?"Mặt bằng":"Căn hộ"},{value:p.locationCode},{value:p.customerName},{value:p.receivableReference},{value:p.serviceDescription},{value:methodLabel(p.paymentMethod)},{value:p.transactionReference},{value:p.amount,type:"Number"},{value:p.note}])).join("");
  const xml=`<?xml version="1.0" encoding="UTF-8"?><?mso-application progid="Excel.Sheet"?><Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Styles><Style ss:ID="Header"><Font ss:Bold="1"/><Interior ss:Color="#D9EAF2" ss:Pattern="Solid"/></Style></Styles><Worksheet ss:Name="Cong no"><Table><Row ss:StyleID="Header">${["Kỳ","Tòa","Nguồn thu","Căn hộ/Mặt bằng","Khách hàng","Mã phải thu","Dịch vụ","Số lượng","Đơn giá","Phải thu","Đã thu","Còn nợ","Hạn thu","Trạng thái"].map(v=>cell(v)).join("")}</Row>${debtRows}</Table></Worksheet><Worksheet ss:Name="So thu tien"><Table><Row ss:StyleID="Header">${["Ngày thu","Kỳ","Tòa","Nguồn thu","Căn hộ/Mặt bằng","Khách hàng","Mã phải thu","Nội dung thu","Phương thức","Mã giao dịch","Số tiền","Ghi chú"].map(v=>cell(v)).join("")}</Row>${paymentRows}</Table></Worksheet></Workbook>`;
  const blob=new Blob(["\ufeff",xml],{type:"application/vnd.ms-excel;charset=utf-8"}),url=URL.createObjectURL(blob),a=document.createElement("a");
  const period=periods.find(p=>String(p.id)===periodId)?.code??"tat-ca";a.href=url;a.download=`Bao-cao-thu-phi-${period}.xls`;a.click();URL.revokeObjectURL(url);
 }
 return <>
  <header><div><h1>Báo cáo & xuất Excel</h1><p>Bảng kê công nợ và sổ thu tiền phục vụ đối soát kế toán.</p></div><button className="primary" onClick={exportExcel} disabled={loading}><Download/>Xuất Excel</button></header>
  {error&&<div className="page-error top-error">{error}</div>}
  <section className="unit-summary report-summary"><article><FileSpreadsheet/><span><b>{money(summary.due)}</b><small>Tổng phải thu</small></span></article><article><Banknote/><span><b>{money(summary.paid)}</b><small>Đã phân bổ thu</small></span></article><article><WalletCards/><span><b>{money(summary.outstanding)}</b><small>Còn phải thu</small></span></article><article><Banknote/><span><b>{money(summary.received)}</b><small>Phát sinh sổ thu</small></span></article></section>
  <section className="panel unit-panel"><div className="toolbar"><select value={periodId} onChange={e=>{setPeriodId(e.target.value);load(e.target.value)}}><option value="">Tất cả kỳ</option>{periods.map(p=><option key={p.id} value={p.id}>{p.code}</option>)}</select><select value={buildingId} onChange={e=>setBuildingId(e.target.value)}><option value="">Toàn bộ tòa</option>{buildings.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select><select value={source} onChange={e=>setSource(e.target.value)}><option value="">Tất cả nguồn thu</option><option value="RESIDENTIAL">Cư dân</option><option value="COMMERCIAL">Mặt bằng</option></select></div>
   <div className="report-tabs"><div><h2>Bảng kê công nợ</h2><small>{filteredBills.length} khoản phải thu · {filteredBills.reduce((s,b)=>s+b.items.length,0)} dòng dịch vụ</small></div><div><h2>Sổ thu tiền</h2><small>{filteredPayments.length} dòng đã thu</small></div></div>
   <div className="table-wrap"><table><thead><tr><th>Kỳ</th><th>Tòa</th><th>Căn hộ/Mặt bằng</th><th>Khách hàng</th><th>Phải thu</th><th>Đã thu</th><th>Còn nợ</th><th>Trạng thái</th></tr></thead><tbody>{filteredBills.map(b=><tr key={b.referenceCode}><td>{b.periodCode}</td><td>{b.buildingName}</td><td>{b.unitCode}</td><td>{b.customerName}</td><td>{money(b.totalAmount)}</td><td>{money(b.paidAmount)}</td><td>{money(b.outstandingAmount)}</td><td><span className={`status ${b.status.toLowerCase()}`}>{b.status}</span></td></tr>)}{!loading&&!filteredBills.length&&<tr><td colSpan={8} className="empty">Chưa có dữ liệu báo cáo.</td></tr>}</tbody></table></div>
  </section>
 </>;
}
