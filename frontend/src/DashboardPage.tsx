import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Banknote, Clock3 } from "lucide-react";
import { api } from "./api";

type Building = { id: number; code: string; name: string };
type Item = { description: string; amount: number; paidAmount: number; outstandingAmount: number };
type Bill = {
  id: number;
  periodCode: string;
  buildingId: number;
  status: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  items: Item[];
};

const currentPeriod = new Date().toISOString().slice(0, 7).replace("-", "");
const money = (value: number) => new Intl.NumberFormat("vi-VN").format(value) + " đ";
const compactMoney = (value: number) =>
  value >= 1_000_000
    ? new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value / 1_000_000) + " tr"
    : new Intl.NumberFormat("vi-VN").format(value) + " đ";

function serviceGroup(description: string) {
  const value = description.toLowerCase();
  if (value.includes("thuê mặt bằng")) return "Thuê mặt bằng";
  if (value.includes("điện công tơ") || value.includes("tiền điện")) return "Điện mặt bằng";
  if (value.includes("phí dịch vụ")) return "Phí dịch vụ";
  if (value.includes("gửi xe") || value.includes("xe máy") || value.includes("ô tô") || value.includes("xe đạp")) return "Gửi xe cư dân";
  return "Dịch vụ khác";
}

export function DashboardPage() {
  const [buildings, setBuildings] = useState<Building[]>([]);
  const [bills, setBills] = useState<Bill[]>([]);
  const [buildingId, setBuildingId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api<Building[]>("/buildings"), api<Bill[]>("/billing/receivables")])
      .then(([buildingRows, billRows]) => {
        setBuildings(buildingRows);
        setBills(billRows);
      })
      .catch((e) => setError(e instanceof Error ? e.message : "Không tải được dữ liệu tổng quan."))
      .finally(() => setLoading(false));
  }, []);

  const shown = useMemo(
    () => bills.filter((bill) => bill.periodCode === currentPeriod && (!buildingId || bill.buildingId === Number(buildingId))),
    [bills, buildingId],
  );
  const summary = useMemo(() => {
    const total = shown.reduce((sum, bill) => sum + bill.totalAmount, 0);
    const paid = shown.reduce((sum, bill) => sum + bill.paidAmount, 0);
    const outstanding = shown.reduce((sum, bill) => sum + bill.outstandingAmount, 0);
    const overdueBills = shown.filter((bill) => bill.status === "OVERDUE");
    return {
      total,
      paid,
      outstanding,
      overdue: overdueBills.reduce((sum, bill) => sum + bill.outstandingAmount, 0),
      overdueCount: overdueBills.length,
      unpaidCount: shown.filter((bill) => bill.status === "UNPAID").length,
      partialCount: shown.filter((bill) => bill.status === "PARTIAL").length,
    };
  }, [shown]);
  const services = useMemo(() => {
    const grouped = new Map<string, number>();
    shown.forEach((bill) => bill.items.forEach((item) => grouped.set(serviceGroup(item.description), (grouped.get(serviceGroup(item.description)) || 0) + item.amount)));
    const rows = [...grouped.entries()].sort((a, b) => b[1] - a[1]);
    const max = Math.max(...rows.map(([, value]) => value), 1);
    return rows.map(([name, value]) => ({ name, value, width: (value / max) * 100 }));
  }, [shown]);

  return (
    <>
      <header>
        <div>
          <h1>Tổng quan vận hành</h1>
          <p>Số liệu thu phí kỳ {currentPeriod.slice(4, 6)}/{currentPeriod.slice(0, 4)}</p>
        </div>
        <select aria-label="Chọn tòa" value={buildingId} onChange={(e) => setBuildingId(e.target.value)}>
          <option value="">Toàn khu</option>
          {buildings.map((building) => <option key={building.id} value={building.id}>{building.name}</option>)}
        </select>
      </header>
      {error && <div className="page-error top-error">{error}</div>}
      <section className="metrics">
        <article><span>Phải thu kỳ này</span><strong>{compactMoney(summary.total)}</strong><small>{shown.length} khoản phải thu</small></article>
        <article><span>Đã thu và đối soát</span><strong>{compactMoney(summary.paid)}</strong><small>{summary.total ? ((summary.paid / summary.total) * 100).toFixed(1) : "0"}% tổng phải thu</small></article>
        <article className="danger"><span>Quá hạn thanh toán</span><strong>{compactMoney(summary.overdue)}</strong><small>{summary.overdueCount} khoản cần nhắc</small></article>
        <article><span>Còn phải thu</span><strong>{compactMoney(summary.outstanding)}</strong><small>{summary.unpaidCount + summary.partialCount} khoản chưa hoàn tất</small></article>
      </section>
      <section className="grid">
        <article className="panel">
          <h2>Khoản thu theo dịch vụ</h2>
          {loading ? <p>Đang tải dữ liệu…</p> : services.length === 0 ? <p>Chưa có khoản phải thu trong kỳ này.</p> : services.map((service) => (
            <div className="service" key={service.name}><span>{service.name}</span><div><i style={{ width: `${service.width}%` }} /></div><b>{compactMoney(service.value)}</b></div>
          ))}
        </article>
        <article className="panel">
          <h2>Cần xử lý trong kỳ</h2>
          <div className="task"><AlertTriangle /><span><b>{summary.overdueCount} khoản phí quá hạn</b><small>Tổng {money(summary.overdue)} cần nhắc thanh toán</small></span><em>Quá hạn</em></div>
          <div className="task"><Clock3 /><span><b>{summary.unpaidCount} khoản chưa thu</b><small>Chưa ghi nhận thanh toán</small></span><em>Theo dõi</em></div>
          <div className="task"><Banknote /><span><b>{summary.partialCount} khoản thu một phần</b><small>Cần thu nốt số tiền còn lại</small></span><em>Công nợ</em></div>
        </article>
      </section>
    </>
  );
}
