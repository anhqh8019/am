export type AuthUser = { id:number; username:string; displayName:string; roles:string[]; passwordChangeRequired:boolean };
export type LoginResponse = { accessToken:string; tokenType:string; expiresIn:number; user:AuthUser };
const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';
const TOKEN_KEY='apartment_access_token', USER_KEY='apartment_user';
export async function login(username:string,password:string):Promise<LoginResponse>{
  const response=await fetch(`${API_URL}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password})});
  if(!response.ok){if(response.status===401)throw new Error('Tài khoản hoặc mật khẩu không đúng.');throw new Error('Không thể kết nối hệ thống. Vui lòng thử lại sau.');}
  return response.json() as Promise<LoginResponse>;
}
export function saveSession(data:LoginResponse,remember:boolean){clearSession();const storage=remember?localStorage:sessionStorage;storage.setItem(TOKEN_KEY,data.accessToken);storage.setItem(USER_KEY,JSON.stringify(data.user));}
export function readUser():AuthUser|null{const raw=sessionStorage.getItem(USER_KEY)??localStorage.getItem(USER_KEY);if(!raw)return null;try{return JSON.parse(raw) as AuthUser}catch{clearSession();return null}}
export function readToken():string|null{return sessionStorage.getItem(TOKEN_KEY)??localStorage.getItem(TOKEN_KEY)}
export function clearSession(){[localStorage,sessionStorage].forEach(s=>{s.removeItem(TOKEN_KEY);s.removeItem(USER_KEY)})}
