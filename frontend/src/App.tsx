import { useState } from 'react';
import { AlertTriangle, Building2, CarFront, DoorOpen, FileText, LayoutDashboard, LogOut, QrCode, Settings, Store, Users, WalletCards } from 'lucide-react';
import { LoginPage } from './LoginPage';
import { AuthUser, clearSession, login, readUser, saveSession } from './auth';
import { UnitsPage } from './UnitsPage';
import { CommercialPage } from './CommercialPage';
import { ResidentsPage } from './ResidentsPage';
import { ServiceTariffsPage } from './ServiceTariffsPage';

const metrics = [
  ['Phải thu kỳ này', '286,4 triệu', 'Phí dịch vụ, xe, thuê và điện'],
  ['Đã thu và đối soát', '219,8 triệu', '76,7% khoản phải thu'],
  ['Quá hạn thanh toán', '42,6 triệu', '31 khách hàng cần nhắc'],
  ['Xe cần kiểm tra', '12 xe', 'Chưa gửi yêu cầu khóa TTZ']
];

const services = [
  ['Phí dịch vụ', 128, 100], ['Xe cư dân', 86, 67], ['Xe vãng lai', 39, 30],
  ['Thuê mặt bằng', 27, 21], ['Điện mặt bằng', 6.4, 5]
];

export function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => readUser());
  const [page, setPage] = useState<'dashboard'|'units'|'residents'|'commercial'|'tariffs'>('dashboard');

  if (!currentUser) {
    return <LoginPage onLogin={async (username, password, remember) => {
      const response = await login(username, password);
      saveSession(response, remember);
      setCurrentUser(response.user);
    }}/>;
  }

  return <div className="shell">
    <aside>
      <div className="brand"><Building2 size={22}/> Quản lý chung cư<small>2 tòa · hơn 400 căn hộ</small></div>
      <nav>
        <button className={page==='dashboard'?'active':''} onClick={()=>setPage('dashboard')}><LayoutDashboard/>Tổng quan</button>
        <button className={page==='units'?'active':''} onClick={()=>setPage('units')}><DoorOpen/>Căn hộ</button>
        <button className={page==='residents'?'active':''} onClick={()=>setPage('residents')}><Users/>Cư dân & chủ hộ</button>
        <button><WalletCards/>Thu phí và công nợ</button>
        <button><CarFront/>Bãi xe</button>
        <button className={page==='commercial'?'active':''} onClick={()=>setPage('commercial')}><Store/>Mặt bằng cho thuê</button>
        <button className={page==='tariffs'?'active':''} onClick={()=>setPage('tariffs')}><Settings/>Dịch vụ & biểu phí</button>
        <button><QrCode/>Thanh toán QR</button>
        <button><FileText/>Báo cáo</button>
        <button onClick={() => { clearSession(); setCurrentUser(null); }}><LogOut/>Đăng xuất</button>
      </nav>
      <div className="signed-user"><span>{currentUser.displayName}</span><small>{currentUser.roles.join(', ')}</small></div>
    </aside>
    <main>
      {page==='units'?<UnitsPage/>:page==='residents'?<ResidentsPage/>:page==='commercial'?<CommercialPage/>:page==='tariffs'?<ServiceTariffsPage/>:<>
      <header><div><h1>Tổng quan vận hành</h1><p>Dữ liệu minh họa cho phiên bản khởi tạo</p></div><select aria-label="Chọn tòa"><option>Toàn khu</option><option>Tòa A</option><option>Tòa B</option></select></header>
      <section className="metrics">{metrics.map(([label,value,note],i)=><article key={label} className={i===2?'danger':''}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</section>
      <section className="grid">
        <article className="panel"><h2>Khoản thu theo dịch vụ</h2>{services.map(([name,value,width])=><div className="service" key={name as string}><span>{name}</span><div><i style={{width:`${width}%`}}/></div><b>{value} tr</b></div>)}</article>
        <article className="panel"><h2>Cần xử lý hôm nay</h2>
          <div className="task"><AlertTriangle/><span><b>18 khoản phí quá hạn</b><small>Đã lên lịch gửi thông báo</small></span><em>Nhắc phí</em></div>
          <div className="task"><CarFront/><span><b>12 xe chờ kiểm tra</b><small>Kiểm tra trước khi khóa TTZ</small></span><em>Duyệt</em></div>
          <div className="task"><QrCode/><span><b>4 giao dịch QR chưa khớp</b><small>Chờ kế toán đối soát</small></span><em>Đối soát</em></div>
        </article>
      </section>
      <section className="panel qr"><div><h2>QR riêng theo khách hàng và dịch vụ</h2><p>Mỗi khoản phải thu có một QR và mã tham chiếu riêng: khách hàng × dịch vụ × kỳ thu.</p></div><button>Tạo QR từng khoản</button></section>
      </>}
    </main>
  </div>
}
