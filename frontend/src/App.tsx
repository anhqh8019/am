import { useState } from "react";
import {
  Building2,
  CarFront,
  DoorOpen,
  FileText,
  LayoutDashboard,
  LogOut,
  Settings,
  Store,
  Users,
  WalletCards,
} from "lucide-react";
import { LoginPage } from "./LoginPage";
import { AuthUser, clearSession, login, readUser, saveSession } from "./auth";
import { UnitsPage } from "./UnitsPage";
import { CommercialPage } from "./CommercialPage";
import { ResidentsPage } from "./ResidentsPage";
import { ServiceTariffsPage } from "./ServiceTariffsPage";
import { BillingPage } from "./BillingPage";
import { DashboardPage } from "./DashboardPage";
import { ReportsPage } from "./ReportsPage";

export function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() =>
    readUser(),
  );
  const [page, setPage] = useState<
    "dashboard" | "units" | "residents" | "commercial" | "tariffs" | "billing" | "reports"
  >("dashboard");
  const [billingSource, setBillingSource] = useState<"" | "COMMERCIAL">("");

  if (!currentUser) {
    return (
      <LoginPage
        onLogin={async (username, password, remember) => {
          const response = await login(username, password);
          saveSession(response, remember);
          setCurrentUser(response.user);
        }}
      />
    );
  }

  return (
    <div className="shell">
      <aside>
        <div className="brand">
          <Building2 size={22} /> Quản lý chung cư
          <small>2 tòa · hơn 400 căn hộ</small>
        </div>
        <nav>
          <button
            className={page === "dashboard" ? "active" : ""}
            onClick={() => setPage("dashboard")}
          >
            <LayoutDashboard />
            Tổng quan
          </button>
          <button
            className={page === "units" ? "active" : ""}
            onClick={() => setPage("units")}
          >
            <DoorOpen />
            Căn hộ
          </button>
          <button
            className={page === "residents" ? "active" : ""}
            onClick={() => setPage("residents")}
          >
            <Users />
            Cư dân & chủ hộ
          </button>
          <button
            className={page === "billing" ? "active" : ""}
            onClick={() => {
              setBillingSource("");
              setPage("billing");
            }}
          >
            <WalletCards />
            Thu phí và công nợ
          </button>
          <button>
            <CarFront />
            Bãi xe TTZ
          </button>
          <button
            className={page === "commercial" ? "active" : ""}
            onClick={() => setPage("commercial")}
          >
            <Store />
            Mặt bằng
          </button>
          <button
            className={page === "tariffs" ? "active" : ""}
            onClick={() => setPage("tariffs")}
          >
            <Settings />
            Dịch vụ & biểu phí
          </button>
          <button className={page === "reports" ? "active" : ""} onClick={() => setPage("reports")}>
            <FileText />
            Báo cáo
          </button>
          <button
            onClick={() => {
              clearSession();
              setCurrentUser(null);
            }}
          >
            <LogOut />
            Đăng xuất
          </button>
        </nav>
        <div className="signed-user">
          <span>{currentUser.displayName}</span>
          <small>{currentUser.roles.join(", ")}</small>
          <small>Phiên bản v0.4.25</small>
        </div>
      </aside>
      <main>
        {page === "units" ? (
          <UnitsPage />
        ) : page === "residents" ? (
          <ResidentsPage />
        ) : page === "commercial" ? (
          <CommercialPage />
        ) : page === "tariffs" ? (
          <ServiceTariffsPage />
        ) : page === "billing" ? (
          <BillingPage initialSourceType={billingSource} />
        ) : page === "reports" ? (
          <ReportsPage />
        ) : (
          <DashboardPage />
        )}
      </main>
    </div>
  );
}
