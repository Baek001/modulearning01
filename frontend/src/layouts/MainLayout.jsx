import { Outlet, Navigate, useLocation } from 'react-router-dom';
import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import { useAuth } from '../contexts/AuthContext';

export default function MainLayout() {
    const { user, loading } = useAuth();
    const location = useLocation();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    if (location.pathname === '/messenger-popup') {
        return <div className="messenger-popup-shell"><Outlet /></div>;
    }

    if (loading) {
        return (
            <div className="app-loading">
                <div className="app-loading-spinner" />
                <p>로딩 중...</p>
            </div>
        );
    }

    if (!user) return <Navigate to="/login" replace />;

    return (
        <div className={`app-layout ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
            <Sidebar collapsed={sidebarCollapsed} onToggle={() => setSidebarCollapsed((v) => !v)} />
            <Topbar />
            <main className="main-content">
                <div className="page-content">
                    <Outlet />
                </div>
            </main>
        </div>
    );
}
