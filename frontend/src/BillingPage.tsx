import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  Banknote,
  CheckCircle2,
  Download,
  FilePlus2,
  QrCode,
  RefreshCw,
  Send,
  Search,
  WalletCards,
  X,
} from "lucide-react";
import { api } from "./api";
type Building = { id: number; code: string; name: string };
type Period = {
  id: number;
  code: string;
  periodStart: string;
  periodEnd: string;
  dueDate: string;
  status: string;
};
type Item = {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  paidAmount: number;
  outstandingAmount: number;
};
type Bill = {
  id: number;
  sourceType: "RESIDENTIAL" | "COMMERCIAL";
  referenceCode: string;
  periodCode: string;
  buildingId: number;
  buildingName: string;
  unitCode: string;
  customerName: string;
  phone: string;
  zaloPhone?: string;
  telegramContact?: string;
  zaloUserId?: string;
  telegramChatId?: string;
  dueDate: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  status: string;
  items: Item[];
};
type BankQr = {id:number;referenceCode:string;amount:number;bankCode:string;bankAccount:string;accountName:string;qrImageUrl:string;status:string;expiresAt:string};
type QrStatus = {referenceCode:string;status:string};
const money = (n: number) => new Intl.NumberFormat("vi-VN").format(n) + " đ";
const currentMonth = new Date().toISOString().slice(0, 7),
  today = new Date().toISOString().slice(0, 10);
function defaultDue() {
  const d = new Date();
  d.setDate(d.getDate() + 10);
  return d.toISOString().slice(0, 10);
}
export function BillingPage({ initialSourceType = "" }: { initialSourceType?: "" | "COMMERCIAL" }) {
  const [periods, setPeriods] = useState<Period[]>([]),
    [bills, setBills] = useState<Bill[]>([]),
    [buildings, setBuildings] = useState<Building[]>([]);
  const [periodId, setPeriodId] = useState(""),
    [status, setStatus] = useState(""),
    [sourceType, setSourceType] = useState<string>(initialSourceType),
    [keyword, setKeyword] = useState(""),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [loading, setLoading] = useState(true);
  const [generateOpen, setGenerateOpen] = useState(false),
    [paymentBill, setPaymentBill] = useState<Bill | null>(null),
    [qrBill, setQrBill] = useState<Bill | null>(null),
    [bankQr, setBankQr] = useState<BankQr | null>(null),
    [creatingQrId, setCreatingQrId] = useState<number | null>(null),
    [selectedQrItems, setSelectedQrItems] = useState<number[]>([]),
    [creatingGroupedQr, setCreatingGroupedQr] = useState(false),
    [sendTarget, setSendTarget] = useState<Bill | "bulk" | null>(null),
    [sendChannel, setSendChannel] = useState("AUTO"),
    [saving, setSaving] = useState(false),
    [refreshingId, setRefreshingId] = useState<number | null>(null);
  const [generateForm, setGenerateForm] = useState({
    month: currentMonth,
    dueDate: defaultDue(),
    buildingId: "",
    refreshExisting: false,
  });
  const [paymentForm, setPaymentForm] = useState({
    grouped: true,
    transactionReference: "",
    note: "",
    lines: [] as {
      itemId: number;
      selected: boolean;
      amount: string;
      paymentMethod: string;
    }[],
  });
  async function loadPeriods() {
    const data = await api<Period[]>("/billing/periods");
    setPeriods(data);
    return data;
  }
  async function loadBills(selected = periodId) {
    setLoading(true);
    try {
      setBills(
        await api<Bill[]>(
          `/billing/receivables${selected ? `?periodId=${selected}` : ""}`,
        ),
      );
      setError("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không tải được công nợ.");
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    Promise.all([loadPeriods(), api<Building[]>("/buildings")])
      .then(([p, b]) => {
        setBuildings(b);
        if (p.length) {
          setPeriodId(String(p[0].id));
          loadBills(String(p[0].id));
        } else loadBills("");
      })
      .catch((e) => setError(e.message));
  }, []);
  useEffect(() => {
    if (!bankQr) return;
    const timer=window.setInterval(async()=>{try{const rows=await api<QrStatus[]>("/qr-payments");const current=rows.find(q=>q.referenceCode===bankQr.referenceCode);if(current?.status==="PAID"){setNotice(`Đã nhận thanh toán QR ${bankQr.referenceCode}.`);setBankQr(null);setQrBill(null);await loadBills();}}catch{/* thử lại ở lần kiểm tra sau */}},5000);
    return()=>window.clearInterval(timer);
  },[bankQr]);
  const shown = useMemo(
    () =>
      bills.filter(
        (b) =>
          (!status || b.status === status) &&
          (!sourceType || b.sourceType === sourceType) &&
          (!keyword.trim() ||
            `${b.unitCode} ${b.customerName} ${b.phone || ""} ${b.referenceCode}`
              .toLowerCase()
              .includes(keyword.trim().toLowerCase())),
      ),
    [bills, status, sourceType, keyword],
  );
  const summary = useMemo(
    () => ({
      total: shown.reduce((s, b) => s + b.totalAmount, 0),
      paid: shown.reduce((s, b) => s + b.paidAmount, 0),
      outstanding: shown.reduce((s, b) => s + b.outstandingAmount, 0),
      overdue: shown
        .filter((b) => b.status === "OVERDUE")
        .reduce((s, b) => s + b.outstandingAmount, 0),
    }),
    [shown],
  );
  async function generate(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const result = await api<{
        periodId: number;
        periodCode: string;
        created: number;
        updated: number;
        skipped: number;
        locked: number;
      }>("/billing/generate", {
        method: "POST",
        body: JSON.stringify({
          ...generateForm,
          buildingId: generateForm.buildingId
            ? Number(generateForm.buildingId)
            : null,
        }),
      });
      setNotice(
        `Đã tạo ${result.created}, cập nhật ${result.updated} khoản phải thu; bỏ qua ${result.skipped} đối tượng${result.locked ? `, giữ nguyên ${result.locked} khoản đã có thanh toán hoặc QR` : ""}.`,
      );
      setGenerateOpen(false);
      await loadPeriods();
      setPeriodId(String(result.periodId));
      await loadBills(String(result.periodId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không sinh được kỳ thu.");
    } finally {
      setSaving(false);
    }
  }
  function openPayment(b: Bill) {
    setPaymentBill(b);
    setPaymentForm({
      grouped: true,
      transactionReference: "",
      note: "",
      lines: b.items
        .filter((i) => i.outstandingAmount > 0)
        .map((i) => ({
          itemId: i.id,
          selected: true,
          amount: String(i.outstandingAmount),
          paymentMethod: "BANK_TRANSFER",
        })),
    });
    setError("");
  }
  function updatePaymentLine(
    itemId: number,
    patch: Partial<{
      selected: boolean;
      amount: string;
      paymentMethod: string;
    }>,
  ) {
    setPaymentForm((f) => ({
      ...f,
      lines: f.lines.map((line) =>
        line.itemId === itemId ? { ...line, ...patch } : line,
      ),
    }));
  }
  function setGroupedMethod(method: string) {
    setPaymentForm((f) => ({
      ...f,
      lines: f.lines.map((line) => ({ ...line, paymentMethod: method })),
    }));
  }
  const selectedPaymentTotal = paymentForm.lines
    .filter((l) => l.selected)
    .reduce((sum, l) => sum + Number(l.amount || 0), 0);
  async function pay(e: FormEvent) {
    e.preventDefault();
    if (!paymentBill) return;
    const lines = paymentForm.lines
      .filter((l) => l.selected)
      .map(({ itemId, amount, paymentMethod }) => ({
        itemId,
        amount: Number(amount),
        paymentMethod,
      }));
    if (!lines.length) {
      setError("Hãy chọn ít nhất một dịch vụ cần thu.");
      return;
    }
    setSaving(true);
    try {
      await api(`/billing/receivables/${paymentBill.id}/payments`, {
        method: "POST",
        body: JSON.stringify({
          grouped: paymentForm.grouped,
          lines,
          transactionReference: paymentForm.transactionReference,
          note: paymentForm.note,
        }),
      });
      setNotice(
        `Đã ghi nhận ${paymentForm.grouped ? "một phiếu thu gộp" : `${lines.length} phiếu thu riêng`} cho ${paymentBill.sourceType === "COMMERCIAL" ? "mặt bằng" : "căn"} ${paymentBill.unitCode}, tổng ${money(selectedPaymentTotal)}.`,
      );
      setPaymentBill(null);
      await loadBills();
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Không ghi nhận được thanh toán.",
      );
    } finally {
      setSaving(false);
    }
  }
  async function refreshBill(b: Bill) {
    if (
      !window.confirm(
        `Tính lại toàn bộ dịch vụ phải thu của ${b.sourceType === "COMMERCIAL" ? "mặt bằng" : "căn"} ${b.unitCode} trong kỳ ${b.periodCode.slice(4, 6)}/${b.periodCode.slice(0, 4)}? QR chưa thanh toán (nếu có) sẽ bị hủy và cần tạo lại.`,
      )
    )
      return;
    setRefreshingId(b.id);
    setError("");
    setNotice("");
    try {
      const updated = await api<Bill>(`/billing/receivables/${b.id}/refresh`, {
        method: "PUT",
      });
      setNotice(
        `Đã cập nhật ${b.sourceType === "COMMERCIAL" ? "mặt bằng" : "căn"} ${b.unitCode}: số tiền phải thu mới là ${money(updated.totalAmount)}.`,
      );
      await loadBills();
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Không cập nhật được khoản phải thu.",
      );
    } finally {
      setRefreshingId(null);
    }
  }
  async function createQr(item: Item) {
    setCreatingQrId(item.id);setError("");
    try {setBankQr(await api<BankQr>(`/qr-payments/items/${item.id}`, {method:"POST"}));}
    catch(e){setError(e instanceof Error?e.message:"Không tạo được QR thanh toán.");}
    finally{setCreatingQrId(null);}
  }
  async function createGroupedQr() {
    if (selectedQrItems.length < 2) {
      setError("Hãy chọn ít nhất hai dịch vụ để tạo QR gộp.");
      return;
    }
    setCreatingGroupedQr(true);
    setError("");
    try {
      setBankQr(
        await api<BankQr>("/qr-payments/grouped", {
          method: "POST",
          body: JSON.stringify({ itemIds: selectedQrItems }),
        }),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không tạo được QR gộp.");
    } finally {
      setCreatingGroupedQr(false);
    }
  }
  async function downloadQr() {
    if (!bankQr) return;
    const filename = `QR-${qrBill?.unitCode || "thanh-toan"}-${bankQr.referenceCode}.png`;
    try {
      const response = await fetch(bankQr.qrImageUrl);
      if (!response.ok) throw new Error("Không tải được ảnh QR");
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      window.open(bankQr.qrImageUrl, "_blank", "noopener,noreferrer");
      setNotice("Ảnh QR đã được mở ở tab mới. Có thể lưu ảnh từ trình duyệt.");
    }
  }
  async function sendQrNotifications(){if(!sendTarget)return;setSaving(true);setError("");try{const path=sendTarget==="bulk"?`/qr-payments/send-bulk${periodId?`?periodId=${periodId}`:""}`:`/qr-payments/send/receivables/${sendTarget.id}`;const result=await api<{sent:number;failed:number;skipped:number}>(path,{method:"POST",body:JSON.stringify({channel:sendChannel})});setNotice(`Đã gửi ${result.sent} QR; lỗi ${result.failed}; bỏ qua ${result.skipped} dịch vụ đã thanh toán.`);setSendTarget(null);}catch(e){setError(e instanceof Error?e.message:"Không gửi được QR.");}finally{setSaving(false);}}
  const label = (s: string) =>
    ({
      UNPAID: "Chưa thu",
      PARTIAL: "Thu một phần",
      PAID: "Đã thu",
      OVERDUE: "Quá hạn",
    })[s] || s;
  return (
    <>
      <header>
        <div>
          <h1>Thu phí và công nợ</h1>
          <p>
            Thu phí căn hộ và tiền thuê mặt bằng theo từng tháng
          </p>
        </div>
        <div className="header-actions"><button className="secondary" disabled={!bills.some(b=>b.outstandingAmount>0)} onClick={()=>{setSendTarget("bulk");setSendChannel("AUTO")}}><Send />Gửi QR hàng loạt</button><button className="primary" onClick={() => setGenerateOpen(true)}><FilePlus2 />Tạo kỳ thu</button></div>
      </header>
      <section className="unit-summary billing-summary">
        <article>
          <WalletCards />
          <span>
            <b>{money(summary.total)}</b>
            <small>Tổng phải thu</small>
          </span>
        </article>
        <article>
          <CheckCircle2 />
          <span>
            <b>{money(summary.paid)}</b>
            <small>Đã thu</small>
          </span>
        </article>
        <article>
          <Banknote />
          <span>
            <b>{money(summary.outstanding)}</b>
            <small>Còn phải thu</small>
          </span>
        </article>
        <article>
          <AlertTriangle />
          <span>
            <b>{money(summary.overdue)}</b>
            <small>Đã quá hạn</small>
          </span>
        </article>
      </section>
      {notice && <div className="success-message">{notice}</div>}
      {error && <div className="page-error top-error">{error}</div>}
      <section className="panel unit-panel">
        <div className="toolbar">
          <select
            value={periodId}
            onChange={(e) => {
              setPeriodId(e.target.value);
              loadBills(e.target.value);
            }}
          >
            <option value="">Tất cả kỳ thu</option>
            {periods.map((p) => (
              <option key={p.id} value={p.id}>
                Kỳ {p.code.slice(4, 6)}/{p.code.slice(0, 4)} · hạn {p.dueDate}
              </option>
            ))}
          </select>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            <option value="UNPAID">Chưa thu</option>
            <option value="PARTIAL">Thu một phần</option>
            <option value="OVERDUE">Quá hạn</option>
            <option value="PAID">Đã thu</option>
          </select>
          <select value={sourceType} onChange={(e) => setSourceType(e.target.value)}>
            <option value="">Tất cả đối tượng</option>
            <option value="RESIDENTIAL">Căn hộ</option>
            <option value="COMMERCIAL">Mặt bằng thuê</option>
          </select>
          <label className="search">
            <Search />
            <input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Căn hộ/mặt bằng, khách hàng, mã thu..."
            />
          </label>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Đối tượng</th>
                <th>Khách hàng</th>
                <th>Kỳ thu</th>
                <th>Phải thu</th>
                <th>Đã thu</th>
                <th>Còn lại</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="empty">
                    Đang tải dữ liệu…
                  </td>
                </tr>
              ) : shown.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty">
                    Chưa có khoản phải thu trong kỳ đã chọn.
                  </td>
                </tr>
              ) : (
                shown.map((b) => (
                  <tr key={b.id}>
                    <td>
                      <b>{b.unitCode}</b>
                      <small className="cell-note">
                        {b.sourceType === "COMMERCIAL" ? "Mặt bằng thuê" : "Căn hộ"} · {b.buildingName} · {b.referenceCode}
                      </small>
                    </td>
                    <td>
                      {b.customerName}
                      <small className="cell-note">
                        {b.phone || "Chưa có SĐT"}
                      </small>
                      {b.sourceType === "RESIDENTIAL" && <small className="contact-channels">{b.zaloPhone&&<em title={b.zaloUserId?"Đã liên kết Zalo OA":"Có SĐT Zalo, chưa liên kết OA"}>Zalo{b.zaloUserId?" ✓":""}</em>}{b.telegramContact&&<em title={b.telegramChatId?"Đã liên kết Telegram Bot":"Có liên hệ Telegram, chưa liên kết Bot"}>Telegram{b.telegramChatId?" ✓":""}</em>}{!b.zaloPhone&&!b.telegramContact&&<span>Chưa có kênh liên hệ</span>}</small>}
                    </td>
                    <td>
                      {b.periodCode.slice(4, 6)}/{b.periodCode.slice(0, 4)}
                      <small className="cell-note">Hạn {b.dueDate}</small>
                    </td>
                    <td>
                      {money(b.totalAmount)}
                      <small className="cell-note">
                        {b.items.map((i) => i.description).join(", ")}
                      </small>
                    </td>
                    <td>{money(b.paidAmount)}</td>
                    <td>
                      <b>{money(b.outstandingAmount)}</b>
                    </td>
                    <td>
                      <span className={`status ${b.status.toLowerCase()}`}>
                        {label(b.status)}
                      </span>
                    </td>
                    <td className="actions billing-actions">
                      {b.paidAmount === 0 && (
                        <button
                          className="refresh-bill"
                          disabled={refreshingId === b.id}
                          onClick={() => refreshBill(b)}
                        >
                          <RefreshCw />
                          {refreshingId === b.id ? "Đang cập nhật" : "Cập nhật"}
                        </button>
                      )}
                      {b.outstandingAmount > 0 && (
                        <button className="qr-button" onClick={() => {setQrBill(b);setBankQr(null);setSelectedQrItems(b.items.filter(i=>i.outstandingAmount>0).map(i=>i.id));}}><QrCode />QR</button>
                      )}
                      {b.sourceType === "RESIDENTIAL" && b.outstandingAmount > 0 && <button className="send-qr-button" onClick={()=>{setSendTarget(b);setSendChannel("AUTO")}}><Send />Gửi</button>}
                      {b.outstandingAmount > 0 && (
                        <button
                          className="collect-button"
                          onClick={() => openPayment(b)}
                        >
                          Thu tiền
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
      {sendTarget && <div className="modal-backdrop"><div className="modal send-qr-modal"><div className="modal-title"><div><h2>{sendTarget==="bulk"?"Gửi QR toàn bộ kỳ thu":"Gửi QR cho chủ căn hộ"}</h2><p>{sendTarget==="bulk"?"Tạo và gửi QR riêng cho từng dịch vụ còn nợ.":`Căn ${sendTarget.unitCode} · ${sendTarget.customerName}`}</p></div><button type="button" onClick={()=>setSendTarget(null)}><X /></button></div><div className="form-grid"><label className="full">Kênh gửi<select value={sendChannel} onChange={e=>setSendChannel(e.target.value)}><option value="AUTO">Tự chọn Zalo hoặc Telegram</option><option value="ZALO">Chỉ gửi Zalo</option><option value="TELEGRAM">Chỉ gửi Telegram</option></select></label><p className="full send-note">Chỉ gửi cho chủ hộ đã có ID kênh tương ứng. Mỗi dịch vụ nhận một QR và mã chuyển khoản riêng.</p></div><div className="modal-actions"><button type="button" onClick={()=>setSendTarget(null)}>Hủy</button><button className="primary" disabled={saving} onClick={sendQrNotifications}><Send />{saving?"Đang gửi…":"Xác nhận gửi"}</button></div></div></div>}
      {qrBill && (
        <div className="modal-backdrop"><div className="modal qr-payment-modal">
          <div className="modal-title"><div><h2>Tạo QR thanh toán</h2><p>{qrBill.sourceType === "COMMERCIAL" ? "Mặt bằng" : "Căn"} {qrBill.unitCode} · chọn từng dịch vụ hoặc gộp nhiều dịch vụ</p></div><button type="button" onClick={() => setQrBill(null)}><X /></button></div>
          {!bankQr ? <><div className="qr-group-toolbar"><div><b>QR gộp</b><small>Đã chọn {selectedQrItems.length} dịch vụ · {money(qrBill.items.filter(i=>selectedQrItems.includes(i.id)).reduce((sum,i)=>sum+i.outstandingAmount,0))}</small></div><button className="primary" disabled={selectedQrItems.length<2||creatingGroupedQr} onClick={createGroupedQr}><QrCode />{creatingGroupedQr?"Đang tạo…":"Tạo 1 QR gộp"}</button></div><div className="qr-service-list">
            {qrBill.items.filter(i => i.outstandingAmount > 0).map(item => <article key={item.id}><label className="qr-service-choice"><input type="checkbox" checked={selectedQrItems.includes(item.id)} onChange={e=>setSelectedQrItems(ids=>e.target.checked?[...ids,item.id]:ids.filter(id=>id!==item.id))}/><span><b>{item.description}</b><small>Còn phải thu {money(item.outstandingAmount)}</small></span></label><button className="secondary" disabled={creatingQrId===item.id||creatingGroupedQr} onClick={() => createQr(item)}><QrCode />{creatingQrId===item.id?"Đang tạo…":"QR riêng"}</button></article>)}
          </div></> : <div className="bank-qr-detail">
            <img src={bankQr.qrImageUrl} alt={`QR thanh toán ${bankQr.referenceCode}`} />
            <div><span>Số tiền</span><b>{money(bankQr.amount)}</b></div><div><span>Nội dung chuyển khoản</span><b>{bankQr.referenceCode}</b></div><div><span>Tài khoản nhận</span><b>{bankQr.bankAccount} · {bankQr.accountName}</b></div>
            <p>Trạng thái sẽ tự chuyển sang đã thanh toán sau khi ngân hàng gửi giao dịch và hệ thống đối soát thành công.</p>
          </div>}
          <div className="modal-actions"><button type="button" onClick={() => bankQr?setBankQr(null):setQrBill(null)}>{bankQr?"Chọn dịch vụ khác":"Đóng"}</button>{bankQr&&<button type="button" className="primary" onClick={downloadQr}><Download />Tải QR về</button>}</div>
        </div></div>
      )}
      {generateOpen && (
        <div className="modal-backdrop">
          <form className="modal" onSubmit={generate}>
            <div className="modal-title">
              <div>
                <h2>Tạo hoặc cập nhật kỳ thu</h2>
                <p>Sinh phí căn hộ, tiền thuê và điện mặt bằng theo tháng.</p>
              </div>
              <button type="button" onClick={() => setGenerateOpen(false)}>
                <X />
              </button>
            </div>
            <div className="form-grid">
              <label>
                Tháng thu
                <input
                  required
                  type="month"
                  value={generateForm.month}
                  onChange={(e) =>
                    setGenerateForm({ ...generateForm, month: e.target.value })
                  }
                />
              </label>
              <label>
                Hạn thanh toán
                <input
                  required
                  type="date"
                  min={`${generateForm.month}-01`}
                  value={generateForm.dueDate}
                  onChange={(e) =>
                    setGenerateForm({
                      ...generateForm,
                      dueDate: e.target.value,
                    })
                  }
                />
              </label>
              <label className="full">
                Phạm vi
                <select
                  value={generateForm.buildingId}
                  onChange={(e) =>
                    setGenerateForm({
                      ...generateForm,
                      buildingId: e.target.value,
                    })
                  }
                >
                  <option value="">Toàn bộ chung cư</option>
                  {buildings.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="full refresh-option">
                <span>
                  <input
                    type="checkbox"
                    checked={generateForm.refreshExisting}
                    onChange={(e) =>
                      setGenerateForm({
                        ...generateForm,
                        refreshExisting: e.target.checked,
                      })
                    }
                  />{" "}
                  Tính lại công nợ chưa thanh toán đã có trong kỳ
                </span>
                <small>
                  Dùng sau khi thay đổi biểu phí hoặc dịch vụ. Khoản đã thu hoặc
                  thu một phần hoặc đã phát hành QR luôn được giữ nguyên. Hợp đồng thuê đang hiệu lực
                  sẽ lấy hạn thanh toán theo ngày thu ghi trong hợp đồng.
                </small>
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" onClick={() => setGenerateOpen(false)}>
                Hủy
              </button>
              <button className="primary" disabled={saving}>
                {saving
                  ? "Đang xử lý…"
                  : generateForm.refreshExisting
                    ? "Tạo và cập nhật"
                    : "Tạo khoản phải thu"}
              </button>
            </div>
          </form>
        </div>
      )}
      {paymentBill && (
        <div className="modal-backdrop">
          <form className="modal payment-modal" onSubmit={pay}>
            <div className="modal-title">
              <div>
                <h2>Ghi nhận thanh toán theo dịch vụ</h2>
                <p>
                  Căn {paymentBill.unitCode} · còn phải thu{" "}
                  {money(paymentBill.outstandingAmount)}
                </p>
              </div>
              <button type="button" onClick={() => setPaymentBill(null)}>
                <X />
              </button>
            </div>
            <div className="payment-mode">
              <label>
                <input
                  type="radio"
                  checked={paymentForm.grouped}
                  onChange={() =>
                    setPaymentForm({ ...paymentForm, grouped: true })
                  }
                />{" "}
                Gộp dịch vụ vào một phiếu thu
              </label>
              <label>
                <input
                  type="radio"
                  checked={!paymentForm.grouped}
                  onChange={() =>
                    setPaymentForm({ ...paymentForm, grouped: false })
                  }
                />{" "}
                Tạo phiếu thu riêng từng dịch vụ
              </label>
            </div>
            {paymentForm.grouped && (
              <label className="grouped-method">
                Phương thức chung
                <select
                  value={paymentForm.lines[0]?.paymentMethod || "BANK_TRANSFER"}
                  onChange={(e) => setGroupedMethod(e.target.value)}
                >
                  <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                  <option value="CASH">Tiền mặt</option>
                  <option value="QR">QR Code</option>
                </select>
              </label>
            )}
            <div className="payment-items">
              {paymentBill.items
                .filter((i) => i.outstandingAmount > 0)
                .map((item) => {
                  const line = paymentForm.lines.find(
                    (l) => l.itemId === item.id,
                  )!;
                  return (
                    <article
                      key={item.id}
                      className={line.selected ? "selected" : ""}
                    >
                      <label className="payment-check">
                        <input
                          type="checkbox"
                          checked={line.selected}
                          onChange={(e) =>
                            updatePaymentLine(item.id, {
                              selected: e.target.checked,
                            })
                          }
                        />
                        <span>
                          <b>{item.description}</b>
                          <small>
                            Phải thu {money(item.amount)} · đã thu{" "}
                            {money(item.paidAmount)} · còn{" "}
                            {money(item.outstandingAmount)}
                          </small>
                        </span>
                      </label>
                      <input
                        aria-label={`Số tiền thu ${item.description}`}
                        disabled={!line.selected}
                        required={line.selected}
                        type="number"
                        min="1"
                        max={item.outstandingAmount}
                        value={line.amount}
                        onChange={(e) =>
                          updatePaymentLine(item.id, { amount: e.target.value })
                        }
                      />
                      {!paymentForm.grouped && (
                        <select
                          disabled={!line.selected}
                          value={line.paymentMethod}
                          onChange={(e) =>
                            updatePaymentLine(item.id, {
                              paymentMethod: e.target.value,
                            })
                          }
                        >
                          <option value="BANK_TRANSFER">Chuyển khoản</option>
                          <option value="CASH">Tiền mặt</option>
                          <option value="QR">QR Code</option>
                        </select>
                      )}
                    </article>
                  );
                })}
            </div>
            <div className="payment-total">
              <span>Tổng tiền thu</span>
              <b>{money(selectedPaymentTotal)}</b>
            </div>
            <div className="form-grid">
              <label className="full">
                Mã giao dịch / tham chiếu
                <input
                  maxLength={100}
                  value={paymentForm.transactionReference}
                  onChange={(e) =>
                    setPaymentForm({
                      ...paymentForm,
                      transactionReference: e.target.value,
                    })
                  }
                />
              </label>
              <label className="full">
                Ghi chú
                <textarea
                  value={paymentForm.note}
                  onChange={(e) =>
                    setPaymentForm({ ...paymentForm, note: e.target.value })
                  }
                />
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" onClick={() => setPaymentBill(null)}>
                Hủy
              </button>
              <button
                className="primary"
                disabled={saving || selectedPaymentTotal <= 0}
              >
                {saving
                  ? "Đang lưu…"
                  : paymentForm.grouped
                    ? "Lập phiếu thu gộp"
                    : "Lập các phiếu thu riêng"}
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
