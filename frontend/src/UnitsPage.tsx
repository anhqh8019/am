import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Building2, Pencil, Plus, Search, X } from 'lucide-react';
import { api } from './api';

type Building = { id:number; code:string; name:string };
type Unit = { id:number; buildingId:number; buildingCode:string; buildingName:string; code:string; unitType:'APARTMENT'|'COMMERCIAL'; areaM2:number|null; status:'ACTIVE'|'INACTIVE' };
type FormState = { buildingId:string; code:string; unitType:'APARTMENT'|'COMMERCIAL'; areaM2:string; status:'ACTIVE'|'INACTIVE' };
const emptyForm:FormState = { buildingId:'', code:'', unitType:'APARTMENT', areaM2:'', status:'ACTIVE' };

export function UnitsPage() {
  const [buildings,setBuildings]=useState<Building[]>([]), [units,setUnits]=useState<Unit[]>([]);
  const [buildingId,setBuildingId]=useState(''), [keyword,setKeyword]=useState('');
  const [loading,setLoading]=useState(true), [error,setError]=useState('');
  const [editing,setEditing]=useState<Unit|null>(null), [form,setForm]=useState<FormState>(emptyForm), [saving,setSaving]=useState(false);

  async function loadUnits() {
    setLoading(true); setError('');
    try {
      const params=new URLSearchParams(); if(buildingId)params.set('buildingId',buildingId); if(keyword.trim())params.set('keyword',keyword.trim());
      setUnits(await api<Unit[]>(`/units${params.size?`?${params}`:''}`));
    } catch(e) { setError(e instanceof Error?e.message:'Không tải được dữ liệu.'); }
    finally { setLoading(false); }
  }
  useEffect(()=>{ api<Building[]>('/buildings').then(data=>{setBuildings(data);if(data.length)setForm(f=>({...f,buildingId:String(data[0].id)}));}).catch(e=>setError(e.message)); },[]);
  useEffect(()=>{ const timer=setTimeout(loadUnits,250); return()=>clearTimeout(timer); },[buildingId,keyword]);

  const counts=useMemo(()=>({total:units.length,apartments:units.filter(x=>x.unitType==='APARTMENT').length,commercial:units.filter(x=>x.unitType==='COMMERCIAL').length}),[units]);
  function openCreate(){setEditing({} as Unit);setForm({...emptyForm,buildingId:String(buildings[0]?.id??'')});setError('');}
  function openEdit(unit:Unit){setEditing(unit);setForm({buildingId:String(unit.buildingId),code:unit.code,unitType:unit.unitType,areaM2:unit.areaM2?.toString()??'',status:unit.status});setError('');}
  async function submit(e:FormEvent){e.preventDefault();setSaving(true);setError('');try{
    const body=JSON.stringify({buildingId:Number(form.buildingId),code:form.code,unitType:form.unitType,areaM2:form.areaM2?Number(form.areaM2):null,status:form.status});
    if(editing?.id)await api(`/units/${editing.id}`,{method:'PUT',body});else await api('/units',{method:'POST',body});
    setEditing(null);await loadUnits();
  }catch(e){setError(e instanceof Error?e.message:'Không thể lưu căn hộ.');}finally{setSaving(false)}}
  async function deactivate(unit:Unit){if(!confirm(`Ngừng hoạt động căn ${unit.code}?`))return;try{await api(`/units/${unit.id}`,{method:'DELETE'});await loadUnits();}catch(e){setError(e instanceof Error?e.message:'Không thể cập nhật.')}}

  return <>
    <header><div><h1>Quản lý căn hộ và mặt bằng</h1><p>Danh mục đơn vị sử dụng thuộc hai tòa nhà</p></div><button className="primary" onClick={openCreate}><Plus/>Thêm căn hộ</button></header>
    <section className="unit-summary"><article><Building2/><span><b>{counts.total}</b><small>Tổng đơn vị đang hiển thị</small></span></article><article><span><b>{counts.apartments}</b><small>Căn hộ</small></span></article><article><span><b>{counts.commercial}</b><small>Mặt bằng kinh doanh</small></span></article></section>
    <section className="panel unit-panel">
      <div className="toolbar"><select value={buildingId} onChange={e=>setBuildingId(e.target.value)}><option value="">Tất cả tòa</option>{buildings.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select><label className="search"><Search/><input value={keyword} onChange={e=>setKeyword(e.target.value)} placeholder="Tìm mã căn hộ..."/></label></div>
      {error&&<div className="page-error">{error}</div>}
      <div className="table-wrap"><table><thead><tr><th>Mã căn</th><th>Tòa nhà</th><th>Loại</th><th>Diện tích</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        {loading?<tr><td colSpan={6} className="empty">Đang tải dữ liệu…</td></tr>:units.length===0?<tr><td colSpan={6} className="empty">Chưa có căn hộ. Hãy tạo dữ liệu đầu tiên.</td></tr>:units.map(unit=><tr key={unit.id}><td><b>{unit.code}</b></td><td>{unit.buildingName}</td><td>{unit.unitType==='APARTMENT'?'Căn hộ':'Mặt bằng'}</td><td>{unit.areaM2?`${unit.areaM2} m²`:'—'}</td><td><span className={`status ${unit.status.toLowerCase()}`}>{unit.status==='ACTIVE'?'Hoạt động':'Ngừng'}</span></td><td className="actions"><button title="Sửa" onClick={()=>openEdit(unit)}><Pencil/></button>{unit.status==='ACTIVE'&&<button className="deactivate" title="Ngừng hoạt động" onClick={()=>deactivate(unit)}><X/></button>}</td></tr>)}
      </tbody></table></div>
    </section>
    {editing&&<div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)setEditing(null)}}><form className="modal" onSubmit={submit}><div className="modal-title"><div><h2>{editing.id?'Cập nhật căn hộ':'Thêm căn hộ'}</h2><p>Thông tin dùng để liên kết cư dân và các khoản phí.</p></div><button type="button" onClick={()=>setEditing(null)}><X/></button></div>
      <div className="form-grid"><label>Tòa nhà<select required value={form.buildingId} onChange={e=>setForm({...form,buildingId:e.target.value})}>{buildings.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select></label><label>Mã căn hộ<input required maxLength={30} value={form.code} onChange={e=>setForm({...form,code:e.target.value})} placeholder="Ví dụ: A-1201"/></label><label>Loại<select value={form.unitType} onChange={e=>setForm({...form,unitType:e.target.value as FormState['unitType']})}><option value="APARTMENT">Căn hộ</option><option value="COMMERCIAL">Mặt bằng kinh doanh</option></select></label><label>Diện tích (m²)<input type="number" min="0.01" step="0.01" value={form.areaM2} onChange={e=>setForm({...form,areaM2:e.target.value})} placeholder="75.5"/></label>{editing.id&&<label>Trạng thái<select value={form.status} onChange={e=>setForm({...form,status:e.target.value as FormState['status']})}><option value="ACTIVE">Hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option></select></label>}</div>
      <div className="modal-actions"><button type="button" onClick={()=>setEditing(null)}>Hủy</button><button className="primary" disabled={saving}>{saving?'Đang lưu…':'Lưu thông tin'}</button></div></form></div>}
  </>;
}
