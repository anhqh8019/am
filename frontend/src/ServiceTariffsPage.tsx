import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Building2, CalendarClock, CircleDollarSign, Plus, X } from 'lucide-react';
import { api } from './api';

type Building = { id:number; code:string; name:string };
type Tariff = {
  id:number; serviceCode:string; buildingId:number|null; buildingName:string;
  unitPrice:number; calculationType:string; effectiveFrom:string;
  effectiveTo:string|null; description:string|null; active:boolean;
};

const today = new Date().toISOString().slice(0,10);
const currency = new Intl.NumberFormat('vi-VN');
const date = (value:string|null) => value ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : 'Không giới hạn';

export function ServiceTariffsPage() {
  const [buildings,setBuildings] = useState<Building[]>([]);
  const [tariffs,setTariffs] = useState<Tariff[]>([]);
  const [showForm,setShowForm] = useState(false);
  const [form,setForm] = useState({buildingId:'',unitPrice:'',effectiveFrom:today,effectiveTo:'',description:''});
  const [loading,setLoading] = useState(true), [saving,setSaving] = useState(false), [error,setError] = useState('');

  async function load() {
    setLoading(true); setError('');
    try {
      const [buildingData,tariffData] = await Promise.all([
        api<Building[]>('/buildings'), api<Tariff[]>('/service-tariffs')
      ]);
      setBuildings(buildingData); setTariffs(tariffData);
    } catch (e) { setError(e instanceof Error ? e.message : 'Không tải được biểu phí.'); }
    finally { setLoading(false); }
  }
  useEffect(()=>{load()},[]);

  const current = useMemo(() => tariffs.filter(t => t.effectiveFrom <= today && (!t.effectiveTo || t.effectiveTo >= today)),[tariffs]);
  const scopes = 1 + buildings.length;

  async function submit(e:FormEvent) {
    e.preventDefault(); setSaving(true); setError('');
    try {
      await api('/service-tariffs',{method:'POST',body:JSON.stringify({
        buildingId:form.buildingId ? Number(form.buildingId) : null,
        unitPrice:Number(form.unitPrice), effectiveFrom:form.effectiveFrom,
        effectiveTo:form.effectiveTo||null, description:form.description.trim()||null
      })});
      setShowForm(false); setForm({buildingId:'',unitPrice:'',effectiveFrom:today,effectiveTo:'',description:''}); await load();
    } catch(e) { setError(e instanceof Error ? e.message : 'Không lưu được biểu phí.'); }
    finally { setSaving(false); }
  }

  function status(t:Tariff) {
    if (t.effectiveFrom > today) return ['scheduled','Đã lên lịch'];
    if (!t.effectiveTo || t.effectiveTo >= today) return ['active','Đang áp dụng'];
    return ['inactive','Hết hiệu lực'];
  }

  return <>
    <header><div><h1>Dịch vụ và biểu phí</h1><p>Quản lý đơn giá dùng chung và lịch sử thay đổi phí</p></div><button className="primary" onClick={()=>{setError('');setShowForm(true)}}><Plus/>Thiết lập đơn giá</button></header>
    <section className="unit-summary tariff-summary">
      <article><CircleDollarSign/><span><b>{current.length}</b><small>Biểu phí đang có hiệu lực</small></span></article>
      <article><Building2/><span><b>{scopes}</b><small>Phạm vi có thể áp dụng</small></span></article>
      <article><CalendarClock/><span><b>{tariffs.length}</b><small>Tổng số phiên bản biểu phí</small></span></article>
    </section>
    {error&&<div className="page-error top-error">{error}</div>}
    <section className="panel unit-panel">
      <div className="tariff-intro"><div><h2>Phí dịch vụ tòa nhà</h2><p>Tự động tính theo diện tích căn hộ × đơn giá đồng/m²/tháng.</p></div><span className="status active">Dịch vụ bắt buộc</span></div>
      <div className="table-wrap"><table><thead><tr><th>Phạm vi</th><th>Đơn giá</th><th>Thời gian hiệu lực</th><th>Mô tả</th><th>Trạng thái</th></tr></thead><tbody>
        {loading?<tr><td colSpan={5} className="empty">Đang tải dữ liệu…</td></tr>:tariffs.length===0?<tr><td colSpan={5} className="empty">Chưa có đơn giá. Hãy thiết lập biểu phí đầu tiên.</td></tr>:tariffs.map(t=>{const [css,label]=status(t);return <tr key={t.id}><td><b>{t.buildingName}</b><small className="cell-note">{t.buildingId?'Áp dụng riêng cho tòa này':'Áp dụng cho toàn bộ Tòa A và Tòa B'}</small></td><td><b>{currency.format(t.unitPrice)} đ</b><small className="cell-note">mỗi m² / tháng</small></td><td><b>{date(t.effectiveFrom)}</b><small className="cell-note">đến {date(t.effectiveTo)}</small></td><td>{t.description||'—'}</td><td><span className={`status ${css}`}>{label}</span></td></tr>})}
      </tbody></table></div>
    </section>
    {showForm&&<div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)setShowForm(false)}}><form className="modal" onSubmit={submit}><div className="modal-title"><div><h2>Thiết lập đơn giá phí dịch vụ</h2><p>Biểu phí hiện tại cùng phạm vi sẽ kết thúc trước ngày giá mới có hiệu lực.</p></div><button type="button" onClick={()=>setShowForm(false)}><X/></button></div>
      <div className="form-grid"><label className="full">Phạm vi áp dụng<select value={form.buildingId} onChange={e=>setForm({...form,buildingId:e.target.value})}><option value="">Toàn bộ chung cư (Tòa A và Tòa B)</option>{buildings.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select></label><label>Ngày bắt đầu áp dụng<input required type="date" value={form.effectiveFrom} onChange={e=>setForm({...form,effectiveFrom:e.target.value})}/></label><label>Ngày kết thúc (không bắt buộc)<input type="date" min={form.effectiveFrom} value={form.effectiveTo} onChange={e=>setForm({...form,effectiveTo:e.target.value})}/></label><label className="full">Đơn giá (đồng/m²/tháng)<input required type="number" min="0" step="1" value={form.unitPrice} onChange={e=>setForm({...form,unitPrice:e.target.value})} placeholder="Ví dụ: 8000"/></label><label className="full">Mô tả<textarea maxLength={500} value={form.description} onChange={e=>setForm({...form,description:e.target.value})} placeholder="Ví dụ: Biểu phí dịch vụ áp dụng từ tháng 10/2026 theo quyết định của Ban quản lý"/></label></div>
      <div className="tariff-preview">Ví dụ căn hộ 51,91 m²: <b>{currency.format(51.91 * Number(form.unitPrice||0))} đ/tháng</b></div>
      <div className="modal-actions"><button type="button" onClick={()=>setShowForm(false)}>Hủy</button><button className="primary" disabled={saving}>{saving?'Đang lưu…':'Áp dụng biểu phí'}</button></div>
    </form></div>}
  </>;
}
