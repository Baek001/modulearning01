import { NavLink, useLocation } from 'react-router-dom';

const navItems = [
    {
        label: '메인', items: [
            {
                path: '/', label: '대시보드',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="3" width="7" height="7" rx="1.5" />
                        <rect x="14" y="3" width="7" height="7" rx="1.5" />
                        <rect x="3" y="14" width="7" height="7" rx="1.5" />
                        <rect x="14" y="14" width="7" height="7" rx="1.5" />
                    </svg>
                )
            },
        ]
    },
    {
        label: '업무', items: [
            {
                path: '/approval', label: '전자결재', badge: 7,
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M9 12l2 2 4-4" />
                        <path d="M4 6h16M4 6v12a2 2 0 002 2h12a2 2 0 002-2V6M9 3h6" />
                    </svg>
                )
            },
            {
                path: '/project', label: '프로젝트',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M2 20h20M5 20V10l7-7 7 7v10M9 20v-6h6v6" />
                    </svg>
                )
            },
            {
                path: '/calendar', label: '일정관리',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="4" width="18" height="18" rx="2" />
                        <path d="M16 2v4M8 2v4M3 10h18" />
                    </svg>
                )
            },
            {
                path: '/attendance', label: '근태관리',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10" />
                        <path d="M12 6v6l4 2" />
                    </svg>
                )
            },
        ]
    },
    {
        label: '소통', items: [
            {
                path: '/email', label: '이메일', badge: 12,
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="2" y="4" width="20" height="16" rx="2" />
                        <path d="M22 7l-10 6L2 7" />
                    </svg>
                )
            },
            {
                path: '/messenger', label: '메신저', badge: 4,
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
                    </svg>
                )
            },
            {
                path: '/board', label: '게시판',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                        <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" />
                    </svg>
                )
            },
            {
                path: '/community', label: '커뮤니티',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="8" cy="8" r="3" />
                        <circle cx="16" cy="8" r="3" />
                        <path d="M3 20a5 5 0 015-5h1a5 5 0 015 5" />
                        <path d="M12 20a5 5 0 015-5h1a5 5 0 015 5" />
                    </svg>
                )
            },
        ]
    },
    {
        label: '관리', items: [
            {
                path: '/organization', label: '조직도',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                        <circle cx="9" cy="7" r="4" />
                        <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
                    </svg>
                )
            },
            {
                path: '/meeting', label: '회의실',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                )
            },
            {
                path: '/mypage', label: '마이페이지',
                icon: (
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                )
            },
        ]
    },
];

export default function Sidebar({ collapsed, onToggle }) {
    const location = useLocation();

    return (
        <aside className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
            <div className="sidebar-logo">
                <div className="logo-icon">ML</div>
                {!collapsed && <h1>모두의 러닝</h1>}
            </div>
            <nav className="sidebar-nav">
                {navItems.map((section) => (
                    <div key={section.label}>
                        {!collapsed && <div className="sidebar-section-label">{section.label}</div>}
                        {section.items.map((item) => (
                            <NavLink
                                key={item.path}
                                to={item.path}
                                className={({ isActive }) =>
                                    `sidebar-link ${isActive && (item.path === '/' ? location.pathname === '/' : true) ? 'active' : ''}`
                                }
                                end={item.path === '/'}
                                title={collapsed ? item.label : undefined}
                            >
                                <span className="link-icon">{item.icon}</span>
                                {!collapsed && <span className="link-text">{item.label}</span>}
                                {!collapsed && item.badge && <span className="link-badge">{item.badge}</span>}
                            </NavLink>
                        ))}
                    </div>
                ))}
            </nav>
            <button type="button" className="sidebar-toggle" onClick={onToggle} aria-label={collapsed ? '사이드바 펼치기' : '사이드바 접기'}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    {collapsed ? <path d="M9 18l6-6-6-6" /> : <path d="M15 18l-6-6 6-6" />}
                </svg>
            </button>
        </aside>
    );
}
