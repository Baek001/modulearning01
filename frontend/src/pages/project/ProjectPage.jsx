import { useEffect, useMemo, useState } from 'react';
import { commonCodeAPI, projectAPI, usersAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const FALLBACK_TYPES = [
  { codeId: 'B201', codeNm: '일반' },
  { codeId: 'B202', codeNm: '신규 구축' },
  { codeId: 'B203', codeNm: '유지보수' },
];

const FALLBACK_PROJECT_STATUSES = [
  { codeId: 'B301', codeNm: '승인 대기' },
  { codeId: 'B302', codeNm: '진행' },
  { codeId: 'B303', codeNm: '보류' },
  { codeId: 'B304', codeNm: '완료' },
  { codeId: 'B305', codeNm: '취소' },
];

const FALLBACK_TASK_STATUSES = [
  { codeId: 'B401', codeNm: '미시작' },
  { codeId: 'B402', codeNm: '진행중' },
  { codeId: 'B403', codeNm: '보류' },
  { codeId: 'B404', codeNm: '완료' },
];

const EMPTY_PROJECT = {
  bizNm: '',
  bizTypeCd: 'B201',
  bizSttsCd: 'B301',
  bizGoal: '',
  bizDetail: '',
  bizScope: '',
  bizBdgt: '',
  strtBizDt: '',
  endBizDt: '',
};

const EMPTY_TASK = {
  taskNm: '',
  bizUserId: '',
  taskSttsCd: 'B401',
  strtTaskDt: '',
  endTaskDt: '',
  taskDetail: '',
};

function pick(list, current = '') {
  if (!list.length) return '';
  if (current && list.some((item) => item.bizId === current)) return current;
  return list[0].bizId;
}

function labelOf(list, code) {
  return list.find((item) => item.codeId === code)?.codeNm || code || '-';
}

function toLocalInput(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const shifted = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return shifted.toISOString().slice(0, 16);
}

function fmt(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function badgeForProject(status) {
  if (status === 'B304') return 'badge-green';
  if (status === 'B302') return 'badge-blue';
  if (status === 'B303') return 'badge-orange';
  if (status === 'B305') return 'badge-red';
  return 'badge-gray';
}

function accentForTask(status) {
  if (status === 'B402') return 'var(--primary-500)';
  if (status === 'B404') return 'var(--success)';
  if (status === 'B403') return 'var(--warning)';
  return 'var(--gray-400)';
}

export default function ProjectPage() {
  const { user } = useAuth();
  const [activeView, setActiveView] = useState('list');
  const [loading, setLoading] = useState(true);
  const [workspaceLoading, setWorkspaceLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [projectDetail, setProjectDetail] = useState(null);
  const [projectMembers, setProjectMembers] = useState([]);
  const [projectAuth, setProjectAuth] = useState('');
  const [tasks, setTasks] = useState([]);

  const [projectTypes, setProjectTypes] = useState(FALLBACK_TYPES);
  const [projectStatuses, setProjectStatuses] = useState(FALLBACK_PROJECT_STATUSES);
  const [taskStatuses, setTaskStatuses] = useState(FALLBACK_TASK_STATUSES);

  const [showProjectModal, setShowProjectModal] = useState(false);
  const [editingProjectId, setEditingProjectId] = useState('');
  const [projectForm, setProjectForm] = useState(EMPTY_PROJECT);
  const [memberAssignments, setMemberAssignments] = useState({});
  const [projectSaving, setProjectSaving] = useState(false);

  const [showTaskModal, setShowTaskModal] = useState(false);
  const [editingTaskId, setEditingTaskId] = useState('');
  const [taskForm, setTaskForm] = useState(EMPTY_TASK);
  const [taskSaving, setTaskSaving] = useState(false);

  const taskColumns = useMemo(() => taskStatuses.filter((item) => item.codeId !== 'B405'), [taskStatuses]);
  const assignableUsers = useMemo(() => users.filter((member) => member.userId !== user?.userId), [users, user?.userId]);
  const canEditProject = projectAuth === 'B101' || projectAuth === 'B102';
  const canManageTasks = projectAuth === 'B101';

  useEffect(() => {
    void bootstrap();
        // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectDetail(null);
      setProjectMembers([]);
      setProjectAuth('');
      setTasks([]);
      return;
    }
    void loadWorkspace(selectedProjectId);
  }, [selectedProjectId]);

  async function bootstrap(preferredProjectId = '') {
    setLoading(true);
    setError('');
    try {
      const [projectRes, userRes, typeRes, projectStatusRes, taskStatusRes] = await Promise.all([
        projectAPI.list(),
        usersAPI.list(),
        commonCodeAPI.list('B2'),
        commonCodeAPI.list('B3'),
        commonCodeAPI.list('B4'),
      ]);
      const nextProjects = projectRes.data || [];
      setProjects(nextProjects);
      setUsers(userRes.data || []);
      setProjectTypes(typeRes.data?.length ? typeRes.data : FALLBACK_TYPES);
      setProjectStatuses(projectStatusRes.data?.length ? projectStatusRes.data : FALLBACK_PROJECT_STATUSES);
      setTaskStatuses(taskStatusRes.data?.length ? taskStatusRes.data : FALLBACK_TASK_STATUSES);
      setSelectedProjectId(pick(nextProjects, preferredProjectId || selectedProjectId));
    } catch (err) {
      setError(err.response?.data?.message || '프로젝트 정보를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }

  async function loadWorkspace(bizId) {
    setWorkspaceLoading(true);
    setError('');
    try {
      const [detailRes, taskRes] = await Promise.all([projectAPI.detail(bizId), projectAPI.tasks(bizId)]);
      setProjectDetail(detailRes.data.project);
      setProjectMembers(detailRes.data.members || []);
      setProjectAuth(detailRes.data.currentUserAuthCd || taskRes.data.currentUserAuthCd || '');
      setTasks(taskRes.data.tasks || []);
    } catch (err) {
      setError(err.response?.data?.message || '프로젝트 작업공간을 불러오지 못했습니다.');
    } finally {
      setWorkspaceLoading(false);
    }
  }
  function openCreateProject() {
    setEditingProjectId('');
    setProjectForm(EMPTY_PROJECT);
    setMemberAssignments({});
    setShowProjectModal(true);
  }

  function openEditProject() {
    if (!projectDetail) return;
    const nextAssignments = {};
    projectMembers.forEach((member) => {
      if (member.bizUserId !== user?.userId) {
        nextAssignments[member.bizUserId] = member.bizAuthCd === 'B103' ? 'B103' : 'B102';
      }
    });
    setEditingProjectId(projectDetail.bizId);
    setProjectForm({
      bizNm: projectDetail.bizNm || '',
      bizTypeCd: projectDetail.bizTypeCd || 'B201',
      bizSttsCd: projectDetail.bizSttsCd || 'B301',
      bizGoal: projectDetail.bizGoal || '',
      bizDetail: projectDetail.bizDetail || '',
      bizScope: projectDetail.bizScope || '',
      bizBdgt: projectDetail.bizBdgt || '',
      strtBizDt: toLocalInput(projectDetail.strtBizDt),
      endBizDt: toLocalInput(projectDetail.endBizDt),
    });
    setMemberAssignments(nextAssignments);
    setShowProjectModal(true);
  }

  function openCreateTask() {
    setEditingTaskId('');
    setTaskForm({
      ...EMPTY_TASK,
      bizUserId: projectMembers.find((member) => member.bizUserId !== user?.userId)?.bizUserId || user?.userId || '',
    });
    setShowTaskModal(true);
  }

  function openEditTask(task) {
    setEditingTaskId(task.taskId);
    setTaskForm({
      taskNm: task.taskNm || '',
      bizUserId: task.bizUserId || '',
      taskSttsCd: task.taskSttsCd || 'B401',
      strtTaskDt: toLocalInput(task.strtTaskDt),
      endTaskDt: toLocalInput(task.endTaskDt),
      taskDetail: task.taskDetail || '',
    });
    setShowTaskModal(true);
  }

  function toggleMember(userId) {
    setMemberAssignments((current) => {
      if (current[userId]) {
        const next = { ...current };
        delete next[userId];
        return next;
      }
      return { ...current, [userId]: 'B102' };
    });
  }

  async function submitProject(event) {
    event.preventDefault();
    setError('');
    setMessage('');

    if (!projectForm.bizNm.trim() || !projectForm.bizGoal.trim()) {
      setError('프로젝트명과 목표는 필수입니다.');
      return;
    }
    if (projectForm.strtBizDt && projectForm.endBizDt && projectForm.strtBizDt > projectForm.endBizDt) {
      setError('프로젝트 종료 일시는 시작 일시보다 빠를 수 없습니다.');
      return;
    }

    const payload = {
      bizNm: projectForm.bizNm.trim(),
      bizTypeCd: projectForm.bizTypeCd,
      bizSttsCd: projectForm.bizSttsCd,
      bizGoal: projectForm.bizGoal.trim(),
      bizDetail: projectForm.bizDetail.trim(),
      bizScope: projectForm.bizScope.trim(),
      bizBdgt: projectForm.bizBdgt ? Number(projectForm.bizBdgt) : null,
      bizPrgrs: 0,
      strtBizDt: projectForm.strtBizDt || null,
      endBizDt: projectForm.endBizDt || null,
      members: Object.entries(memberAssignments).map(([bizUserId, bizAuthCd]) => ({ bizUserId, bizAuthCd })),
    };

    try {
      setProjectSaving(true);
      if (editingProjectId) {
        await projectAPI.update(editingProjectId, payload);
        await bootstrap(editingProjectId);
        await loadWorkspace(editingProjectId);
        setMessage('프로젝트를 수정했습니다.');
      } else {
        const response = await projectAPI.create(payload);
        await bootstrap(response.data.bizId || '');
        setMessage('프로젝트를 생성했습니다.');
      }
      setShowProjectModal(false);
    } catch (err) {
      setError(err.response?.data?.message || '프로젝트 저장에 실패했습니다.');
    } finally {
      setProjectSaving(false);
    }
  }

  async function changeProjectStatus(code) {
    if (!selectedProjectId) return;
    try {
      await projectAPI.setStatus(selectedProjectId, code);
      setMessage('프로젝트 상태를 변경했습니다.');
      await bootstrap(code === 'B305' ? '' : selectedProjectId);
      if (code !== 'B305') await loadWorkspace(selectedProjectId);
    } catch (err) {
      setError(err.response?.data?.message || '프로젝트 상태 변경에 실패했습니다.');
    }
  }

  async function submitTask(event) {
    event.preventDefault();
    setError('');
    setMessage('');

    if (!taskForm.taskNm.trim() || !taskForm.bizUserId) {
      setError('업무명과 담당자는 필수입니다.');
      return;
    }
    if (!taskForm.strtTaskDt || !taskForm.endTaskDt) {
      setError('업무 시작 일시와 종료 일시를 입력하세요.');
      return;
    }
    if (taskForm.strtTaskDt > taskForm.endTaskDt) {
      setError('업무 종료 일시는 시작 일시보다 빠를 수 없습니다.');
      return;
    }

    const payload = {
      taskNm: taskForm.taskNm.trim(),
      bizUserId: taskForm.bizUserId,
      taskSttsCd: taskForm.taskSttsCd,
      strtTaskDt: taskForm.strtTaskDt,
      endTaskDt: taskForm.endTaskDt,
      taskDetail: taskForm.taskDetail.trim(),
      taskPrgrs: 0,
    };

    try {
      setTaskSaving(true);
      if (editingTaskId) {
        await projectAPI.updateTask(editingTaskId, payload);
        setMessage('업무를 수정했습니다.');
      } else {
        await projectAPI.createTask(selectedProjectId, payload);
        setMessage('업무를 생성했습니다.');
      }
      await loadWorkspace(selectedProjectId);
      setShowTaskModal(false);
    } catch (err) {
      setError(err.response?.data?.message || '업무 저장에 실패했습니다.');
    } finally {
      setTaskSaving(false);
    }
  }

  async function changeTaskStatus(taskId, taskSttsCd) {
    try {
      await projectAPI.setTaskStatus(taskId, taskSttsCd);
      setMessage('업무 상태를 변경했습니다.');
      await loadWorkspace(selectedProjectId);
    } catch (err) {
      setError(err.response?.data?.message || '업무 상태 변경에 실패했습니다.');
    }
  }

  async function deleteTask(taskId) {
    if (!window.confirm('이 업무를 삭제하시겠습니까?')) return;
    try {
      await projectAPI.deleteTask(taskId);
      setMessage('업무를 삭제했습니다.');
      await loadWorkspace(selectedProjectId);
    } catch (err) {
      setError(err.response?.data?.message || '업무 삭제에 실패했습니다.');
    }
  }

  if (loading) {
    return (
      <div className="empty-state">
        <div className="empty-icon">...</div>
        <h3>프로젝트 정보를 불러오는 중입니다.</h3>
      </div>
    );
  }

  return (
    <div className="animate-slide-up">
      <div className="page-header">
        <div>
          <h2>프로젝트</h2>
          <p>실제 백엔드와 연결된 프로젝트와 업무를 관리합니다.</p>
        </div>
        <div className="page-header-actions" style={{ display: 'flex', gap: 'var(--spacing-sm)', alignItems: 'center', flexWrap: 'wrap' }}>
          <div className="tabs">
            <button className={`tab-btn ${activeView === 'list' ? 'active' : ''}`} onClick={() => setActiveView('list')}>목록</button>
            <button className={`tab-btn ${activeView === 'kanban' ? 'active' : ''}`} onClick={() => setActiveView('kanban')}>칸반</button>
          </div>
          <button className="btn btn-primary" onClick={openCreateProject}>새 프로젝트</button>
        </div>
      </div>

      {message && <div className="card" style={{ marginBottom: 'var(--spacing-md)', border: '1px solid var(--success)' }}><div className="card-body" style={{ color: 'var(--success)' }}>{message}</div></div>}
      {error && <div className="card" style={{ marginBottom: 'var(--spacing-md)', border: '1px solid #ffc9c9' }}><div className="card-body" style={{ color: '#c92a2a' }}>{error}</div></div>}
      {activeView === 'list' ? (
        projects.length === 0 ? (
          <div className="card"><div className="empty-state"><div className="empty-icon">[]</div><h3>아직 등록된 프로젝트가 없습니다.</h3><p>첫 프로젝트를 생성하면 일정과 업무 흐름을 바로 연결할 수 있습니다.</p><button className="btn btn-primary" onClick={openCreateProject}>프로젝트 만들기</button></div></div>
        ) : (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 'var(--spacing-md)', marginBottom: 'var(--spacing-lg)' }}>
              {projects.map((project) => (
                <div key={project.bizId} className="card" onClick={() => setSelectedProjectId(project.bizId)} style={{ cursor: 'pointer', border: selectedProjectId === project.bizId ? '1px solid var(--primary-300)' : '1px solid transparent' }}>
                  <div className="card-body">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
                      <div>
                        <h3 style={{ fontSize: 'var(--font-size-md)', fontWeight: 600, marginBottom: '6px' }}>{project.bizNm}</h3>
                        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>{labelOf(projectTypes, project.bizTypeCd) || project.bizTypeNm}</div>
                      </div>
                      <span className={`badge ${badgeForProject(project.bizSttsCd)}`}>{labelOf(projectStatuses, project.bizSttsCd)}</span>
                    </div>
                    <div className="progress-bar" style={{ marginBottom: 'var(--spacing-sm)' }}><div className="progress-bar-fill" style={{ width: `${project.bizPrgrs || 0}%` }} /></div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}><span>진행률 {project.bizPrgrs || 0}%</span><span>책임자 {project.bizPicNm || project.bizPicId}</span></div>
                    <div style={{ marginTop: 'var(--spacing-sm)', fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>{fmt(project.strtBizDt)} ~ {fmt(project.endBizDt)}</div>
                  </div>
                </div>
              ))}
            </div>

            {selectedProjectId && !workspaceLoading && projectDetail && (
              <div className="grid-2">
                <div className="card">
                  <div className="card-header"><h3>{projectDetail.bizNm}</h3><div style={{ display: 'flex', gap: 'var(--spacing-sm)', alignItems: 'center', flexWrap: 'wrap' }}><span className={`badge ${badgeForProject(projectDetail.bizSttsCd)}`}>{labelOf(projectStatuses, projectDetail.bizSttsCd)}</span>{canEditProject && <button className="btn btn-sm btn-secondary" onClick={openEditProject}>수정</button>}</div></div>
                  <div className="card-body">
                    <div style={{ display: 'grid', gap: 'var(--spacing-md)' }}>
                      <div><div className="form-label">목표</div><div>{projectDetail.bizGoal || '-'}</div></div>
                      <div><div className="form-label">상세 설명</div><div>{projectDetail.bizDetail || '-'}</div></div>
                      <div><div className="form-label">범위</div><div>{projectDetail.bizScope || '-'}</div></div>
                      <div style={{ display: 'flex', gap: 'var(--spacing-md)', flexWrap: 'wrap' }}><div><div className="form-label">예산</div><div>{projectDetail.bizBdgt ? `${Number(projectDetail.bizBdgt).toLocaleString()}원` : '-'}</div></div><div><div className="form-label">기간</div><div>{fmt(projectDetail.strtBizDt)} ~ {fmt(projectDetail.endBizDt)}</div></div></div>
                      {projectAuth === 'B101' && (
                        <div><label className="form-label" htmlFor="project-status">프로젝트 상태</label><select id="project-status" className="form-input" value={projectDetail.bizSttsCd || 'B301'} onChange={(event) => changeProjectStatus(event.target.value)}>{projectStatuses.map((status) => <option key={status.codeId} value={status.codeId}>{status.codeNm}</option>)}</select></div>
                      )}
                    </div>
                  </div>
                </div>
                <div className="card">
                  <div className="card-header"><h3>참여자</h3><span className="badge badge-gray">{projectMembers.length}명</span></div>
                  <div className="card-body" style={{ padding: 0 }}>
                    {projectMembers.length === 0 ? <div className="empty-state"><div className="empty-icon">--</div><h3>참여자가 없습니다.</h3></div> : projectMembers.map((member) => (
                      <div key={member.bizUserId} className="list-item"><div className="avatar" style={{ background: 'var(--primary-50)', color: 'var(--primary-700)' }}>{(member.bizUserNm || member.bizUserId).slice(0, 1)}</div><div style={{ flex: 1 }}><div style={{ fontWeight: 600 }}>{member.bizUserNm || member.bizUserId}</div><div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>{[member.bizUserDeptNm, member.bizUserJobNm].filter(Boolean).join(' / ')}</div></div><span className="badge badge-gray">{member.bizAuthNm || member.bizAuthCd}</span></div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </>
        )
      ) : projects.length === 0 ? (
        <div className="card"><div className="empty-state"><div className="empty-icon">[]</div><h3>칸반을 보려면 먼저 프로젝트를 생성하세요.</h3><button className="btn btn-primary" onClick={openCreateProject}>프로젝트 만들기</button></div></div>
      ) : (
        <>
          <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
            <div className="card-body" style={{ display: 'flex', gap: 'var(--spacing-md)', alignItems: 'center', flexWrap: 'wrap' }}>
              <div style={{ minWidth: '260px', flex: 1 }}><label className="form-label" htmlFor="project-select">칸반 대상 프로젝트</label><select id="project-select" className="form-input" value={selectedProjectId} onChange={(event) => setSelectedProjectId(event.target.value)}>{projects.map((project) => <option key={project.bizId} value={project.bizId}>{project.bizNm}</option>)}</select></div>
              <div style={{ display: 'flex', gap: 'var(--spacing-sm)', alignItems: 'end', flexWrap: 'wrap' }}><span className="badge badge-gray">권한 {projectAuth || '-'}</span>{canManageTasks && <button className="btn btn-primary" onClick={openCreateTask}>업무 추가</button>}</div>
            </div>
          </div>

          {workspaceLoading ? <div className="card"><div className="empty-state"><div className="empty-icon">...</div><h3>업무 보드를 불러오는 중입니다.</h3></div></div> : (
            <div className="kanban-board">
              {taskColumns.map((column) => {
                const columnTasks = tasks.filter((task) => task.taskSttsCd === column.codeId);
                return (
                  <div key={column.codeId} className="kanban-column">
                    <div className="kanban-column-header" style={{ borderColor: accentForTask(column.codeId) }}><h4>{column.codeNm}</h4><span className="badge badge-gray">{columnTasks.length}</span></div>
                    {columnTasks.length === 0 ? <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>이 상태의 업무가 없습니다.</div> : columnTasks.map((task) => {
                      const canChangeStatus = projectAuth === 'B101' || (projectAuth === 'B102' && task.bizUserId === user?.userId);
                      return <div key={task.taskId} className="kanban-card"><h5>{task.taskNm}</h5><p>담당자 {task.bizUserNm || task.bizUserId}</p><p>{fmt(task.strtTaskDt)} ~ {fmt(task.endTaskDt)}</p><div style={{ marginTop: 'var(--spacing-sm)' }}><label className="form-label" htmlFor={`task-${task.taskId}`}>상태</label><select id={`task-${task.taskId}`} className="form-input" value={task.taskSttsCd} disabled={!canChangeStatus} onChange={(event) => changeTaskStatus(task.taskId, event.target.value)}>{taskColumns.map((status) => <option key={status.codeId} value={status.codeId}>{status.codeNm}</option>)}</select></div>{task.taskDetail && <div style={{ marginTop: 'var(--spacing-sm)', fontSize: 'var(--font-size-xs)', color: 'var(--gray-600)' }}>{task.taskDetail}</div>}{canManageTasks && <div style={{ display: 'flex', gap: 'var(--spacing-sm)', marginTop: 'var(--spacing-md)' }}><button className="btn btn-sm btn-secondary" onClick={() => openEditTask(task)}>수정</button><button className="btn btn-sm btn-outline" onClick={() => deleteTask(task.taskId)}>삭제</button></div>}</div>;
                    })}
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
      {showProjectModal && (
        <div className="modal-overlay" onClick={() => setShowProjectModal(false)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header"><h3>{editingProjectId ? '프로젝트 수정' : '새 프로젝트'}</h3><button className="btn btn-sm btn-secondary" onClick={() => setShowProjectModal(false)}>닫기</button></div>
            <form onSubmit={submitProject}>
              <div className="modal-body">
                <div className="form-group"><label className="form-label" htmlFor="bizNm">프로젝트명</label><input id="bizNm" className="form-input" value={projectForm.bizNm} onChange={(event) => setProjectForm((current) => ({ ...current, bizNm: event.target.value }))} /></div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label" htmlFor="bizTypeCd">유형</label><select id="bizTypeCd" className="form-input" value={projectForm.bizTypeCd} onChange={(event) => setProjectForm((current) => ({ ...current, bizTypeCd: event.target.value }))}>{projectTypes.map((type) => <option key={type.codeId} value={type.codeId}>{type.codeNm}</option>)}</select></div>
                  <div className="form-group"><label className="form-label" htmlFor="bizSttsCd">상태</label><select id="bizSttsCd" className="form-input" value={projectForm.bizSttsCd} disabled={!editingProjectId} onChange={(event) => setProjectForm((current) => ({ ...current, bizSttsCd: event.target.value }))}>{projectStatuses.map((status) => <option key={status.codeId} value={status.codeId}>{status.codeNm}</option>)}</select></div>
                </div>
                <div className="form-group"><label className="form-label" htmlFor="bizGoal">목표</label><textarea id="bizGoal" className="form-input" value={projectForm.bizGoal} onChange={(event) => setProjectForm((current) => ({ ...current, bizGoal: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label" htmlFor="bizDetail">상세 설명</label><textarea id="bizDetail" className="form-input" value={projectForm.bizDetail} onChange={(event) => setProjectForm((current) => ({ ...current, bizDetail: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label" htmlFor="bizScope">범위</label><textarea id="bizScope" className="form-input" value={projectForm.bizScope} onChange={(event) => setProjectForm((current) => ({ ...current, bizScope: event.target.value }))} /></div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label" htmlFor="bizBdgt">예산</label><input id="bizBdgt" type="number" min="0" className="form-input" value={projectForm.bizBdgt} onChange={(event) => setProjectForm((current) => ({ ...current, bizBdgt: event.target.value }))} /></div>
                  <div className="form-group"><label className="form-label">책임자</label><div className="form-input" style={{ display: 'flex', alignItems: 'center' }}>{user?.userNm || user?.userId || '-'}</div></div>
                </div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label" htmlFor="strtBizDt">시작 일시</label><input id="strtBizDt" type="datetime-local" className="form-input" value={projectForm.strtBizDt} onChange={(event) => setProjectForm((current) => ({ ...current, strtBizDt: event.target.value }))} /></div>
                  <div className="form-group"><label className="form-label" htmlFor="endBizDt">종료 일시</label><input id="endBizDt" type="datetime-local" className="form-input" value={projectForm.endBizDt} onChange={(event) => setProjectForm((current) => ({ ...current, endBizDt: event.target.value }))} /></div>
                </div>
                <div className="form-group"><label className="form-label">참여자</label><div style={{ display: 'grid', gap: 'var(--spacing-sm)' }}>{assignableUsers.length === 0 ? <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>추가할 사용자가 없습니다.</div> : assignableUsers.map((member) => { const checked = Boolean(memberAssignments[member.userId]); return <div key={member.userId} style={{ display: 'grid', gridTemplateColumns: 'auto 1fr 140px', gap: 'var(--spacing-sm)', alignItems: 'center' }}><input type="checkbox" checked={checked} onChange={() => toggleMember(member.userId)} /><div><div style={{ fontWeight: 600 }}>{member.userNm}</div><div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)' }}>{[member.deptNm, member.jbgdNm].filter(Boolean).join(' / ')}</div></div><select className="form-input" disabled={!checked} value={memberAssignments[member.userId] || 'B102'} onChange={(event) => setMemberAssignments((current) => ({ ...current, [member.userId]: event.target.value }))}><option value="B102">팀원</option><option value="B103">열람자</option></select></div>; })}</div></div>
              </div>
              <div className="modal-footer"><button type="button" className="btn btn-secondary" onClick={() => setShowProjectModal(false)}>취소</button><button type="submit" className="btn btn-primary" disabled={projectSaving}>{projectSaving ? '저장 중...' : editingProjectId ? '수정 저장' : '프로젝트 생성'}</button></div>
            </form>
          </div>
        </div>
      )}

      {showTaskModal && (
        <div className="modal-overlay" onClick={() => setShowTaskModal(false)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header"><h3>{editingTaskId ? '업무 수정' : '업무 추가'}</h3><button className="btn btn-sm btn-secondary" onClick={() => setShowTaskModal(false)}>닫기</button></div>
            <form onSubmit={submitTask}>
              <div className="modal-body">
                <div className="form-group"><label className="form-label" htmlFor="taskNm">업무명</label><input id="taskNm" className="form-input" value={taskForm.taskNm} onChange={(event) => setTaskForm((current) => ({ ...current, taskNm: event.target.value }))} /></div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label" htmlFor="bizUserId">담당자</label><select id="bizUserId" className="form-input" value={taskForm.bizUserId} onChange={(event) => setTaskForm((current) => ({ ...current, bizUserId: event.target.value }))}><option value="">담당자를 선택하세요</option>{projectMembers.map((member) => <option key={member.bizUserId} value={member.bizUserId}>{member.bizUserNm || member.bizUserId}</option>)}</select></div>
                  <div className="form-group"><label className="form-label" htmlFor="taskSttsCd">상태</label><select id="taskSttsCd" className="form-input" value={taskForm.taskSttsCd} onChange={(event) => setTaskForm((current) => ({ ...current, taskSttsCd: event.target.value }))}>{taskColumns.map((status) => <option key={status.codeId} value={status.codeId}>{status.codeNm}</option>)}</select></div>
                </div>
                <div className="grid-2">
                  <div className="form-group"><label className="form-label" htmlFor="strtTaskDt">시작 일시</label><input id="strtTaskDt" type="datetime-local" className="form-input" value={taskForm.strtTaskDt} onChange={(event) => setTaskForm((current) => ({ ...current, strtTaskDt: event.target.value }))} /></div>
                  <div className="form-group"><label className="form-label" htmlFor="endTaskDt">종료 일시</label><input id="endTaskDt" type="datetime-local" className="form-input" value={taskForm.endTaskDt} onChange={(event) => setTaskForm((current) => ({ ...current, endTaskDt: event.target.value }))} /></div>
                </div>
                <div className="form-group"><label className="form-label" htmlFor="taskDetail">상세 설명</label><textarea id="taskDetail" className="form-input" value={taskForm.taskDetail} onChange={(event) => setTaskForm((current) => ({ ...current, taskDetail: event.target.value }))} /></div>
              </div>
              <div className="modal-footer"><button type="button" className="btn btn-secondary" onClick={() => setShowTaskModal(false)}>취소</button><button type="submit" className="btn btn-primary" disabled={taskSaving}>{taskSaving ? '저장 중...' : editingTaskId ? '수정 저장' : '업무 생성'}</button></div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
