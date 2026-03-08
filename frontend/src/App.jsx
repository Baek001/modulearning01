import { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import MainLayout from './layouts/MainLayout';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const OrgChartPage = lazy(() => import('./pages/organization/OrgChartPage'));
const ApprovalListPage = lazy(() => import('./pages/approval/ApprovalListPage'));
const ContractWorkspacePage = lazy(() => import('./pages/contract/ContractWorkspacePage'));
const ContractTemplateDesignerPage = lazy(() => import('./pages/contract/ContractTemplateDesignerPage'));
const ContractSignPage = lazy(() => import('./pages/contract/ContractSignPage'));
const BoardListPage = lazy(() => import('./pages/board/BoardListPage'));
const AttendancePage = lazy(() => import('./pages/attendance/AttendancePage'));
const CalendarPage = lazy(() => import('./pages/calendar/CalendarPage'));
const CommunityPage = lazy(() => import('./pages/community/CommunityPage'));
const EmailPage = lazy(() => import('./pages/email/EmailPage'));
const MessengerPage = lazy(() => import('./pages/messenger/MessengerPage'));
const ProjectPage = lazy(() => import('./pages/project/ProjectPage'));
const MeetingPage = lazy(() => import('./pages/meeting/MeetingPage'));
const MyPage = lazy(() => import('./pages/mypage/MyPage'));

function RouteFallback() {
  return (
    <div className="app-loading">
      <div className="app-loading-spinner" />
      <p>화면을 불러오는 중...</p>
    </div>
  );
}

function RoutedPage({ children }) {
  return <Suspense fallback={<RouteFallback />}>{children}</Suspense>;
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<RoutedPage><LoginPage /></RoutedPage>} />
          <Route path="/contract/sign/:token" element={<RoutedPage><ContractSignPage /></RoutedPage>} />
          <Route element={<MainLayout />}>
            <Route path="/" element={<RoutedPage><DashboardPage /></RoutedPage>} />
            <Route path="/organization" element={<RoutedPage><OrgChartPage /></RoutedPage>} />
            <Route path="/approval" element={<RoutedPage><ApprovalListPage /></RoutedPage>} />
            <Route path="/approval/contracts" element={<RoutedPage><ContractWorkspacePage /></RoutedPage>} />
            <Route path="/approval/contracts/templates/:templateId?" element={<RoutedPage><ContractTemplateDesignerPage /></RoutedPage>} />
            <Route path="/board" element={<RoutedPage><BoardListPage /></RoutedPage>} />
            <Route path="/attendance" element={<RoutedPage><AttendancePage /></RoutedPage>} />
            <Route path="/calendar" element={<RoutedPage><CalendarPage /></RoutedPage>} />
            <Route path="/community" element={<RoutedPage><CommunityPage /></RoutedPage>} />
            <Route path="/email" element={<RoutedPage><EmailPage /></RoutedPage>} />
            <Route path="/messenger" element={<RoutedPage><MessengerPage /></RoutedPage>} />
            <Route path="/project" element={<RoutedPage><ProjectPage /></RoutedPage>} />
            <Route path="/meeting" element={<RoutedPage><MeetingPage /></RoutedPage>} />
            <Route path="/mypage" element={<RoutedPage><MyPage /></RoutedPage>} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
