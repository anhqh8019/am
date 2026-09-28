import { FormEvent, useState } from 'react';
import { Building2, Eye, EyeOff, LockKeyhole, ShieldCheck, UserRound } from 'lucide-react';

type LoginPageProps = {
  onLogin: (username: string, password: string, remember: boolean) => Promise<void>;
};

export function LoginPage({ onLogin }: LoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError('Vui lòng nhập đầy đủ tài khoản và mật khẩu.');
      return;
    }
    setSubmitting(true);
    try { await onLogin(username.trim(), password, remember); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Đăng nhập không thành công.'); }
    finally { setSubmitting(false); }
  }

  return <main className="login-page">
    <section className="login-intro">
      <div className="login-brand"><Building2 aria-hidden="true"/><span>Apartment Management</span></div>
      <div className="intro-copy">
        <span className="eyebrow">Hệ thống quản lý vận hành chung cư</span>
        <h1>Quản lý tập trung.<br/>Đối soát minh bạch.</h1>
        <p>Quản lý hơn 400 căn hộ, dịch vụ, bãi xe TTZ và mặt bằng kinh doanh trên cùng một nền tảng.</p>
        <div className="intro-points">
          <span><ShieldCheck/>Phân quyền và lưu nhật ký thao tác</span>
          <span><ShieldCheck/>Theo dõi công nợ và nhắc phí đúng hạn</span>
          <span><ShieldCheck/>QR riêng theo khách hàng và dịch vụ</span>
        </div>
      </div>
      <small>Phiên bản khởi tạo 0.1.0</small>
    </section>

    <section className="login-area">
      <form className="login-card" onSubmit={submit} noValidate>
        <div className="mobile-brand"><Building2/><span>Apartment Management</span></div>
        <header>
          <h2>Đăng nhập</h2>
          <p>Sử dụng tài khoản được Ban quản lý cấp.</p>
        </header>

        <label htmlFor="username">Tài khoản</label>
        <div className="input-wrap">
          <UserRound aria-hidden="true"/>
          <input id="username" autoComplete="username" value={username}
                 onChange={e => { setUsername(e.target.value); setError(''); }}
                 placeholder="Nhập tên đăng nhập" autoFocus/>
        </div>

        <label htmlFor="password">Mật khẩu</label>
        <div className="input-wrap">
          <LockKeyhole aria-hidden="true"/>
          <input id="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password"
                 value={password} onChange={e => { setPassword(e.target.value); setError(''); }}
                 placeholder="Nhập mật khẩu"/>
          <button className="password-toggle" type="button" onClick={() => setShowPassword(value => !value)}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
            {showPassword ? <EyeOff/> : <Eye/>}
          </button>
        </div>

        <div className="login-options">
          <label className="remember"><input type="checkbox" checked={remember} onChange={e => setRemember(e.target.checked)}/>Ghi nhớ đăng nhập</label>
          <button type="button" className="text-button">Quên mật khẩu?</button>
        </div>

        {error && <div className="login-error" role="alert">{error}</div>}
        <button className="login-submit" type="submit" disabled={submitting}>{submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        <p className="support">Cần hỗ trợ? Liên hệ quản trị hệ thống của Ban quản lý.</p>
      </form>
    </section>
  </main>;
}
