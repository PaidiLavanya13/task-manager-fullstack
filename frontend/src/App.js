import { useEffect, useState, useCallback } from "react";
import axios from "axios";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080";

const api = axios.create({ baseURL: API });
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem("token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

const PRIORITY_META = {
  HIGH:   { color: "#ef4444", bg: "#fef2f2", label: "High" },
  MEDIUM: { color: "#f59e0b", bg: "#fffbeb", label: "Medium" },
  LOW:    { color: "#22c55e", bg: "#f0fdf4", label: "Low" },
};
const STATUS_META = {
  TODO:        { color: "#6366f1", bg: "#eef2ff", label: "Todo" },
  IN_PROGRESS: { color: "#f59e0b", bg: "#fffbeb", label: "In Progress" },
  DONE:        { color: "#22c55e", bg: "#f0fdf4", label: "Done" },
};
const CATEGORIES = ["WORK", "PERSONAL", "HEALTH", "SHOPPING", "OTHER"];

export default function App() {
  const [screen, setScreen]       = useState("login");
  const [authForm, setAuthForm]   = useState({ username: "", password: "" });
  const [authError, setAuthError] = useState("");
  const [user, setUser]           = useState(null);

  const [tasks, setTasks]         = useState([]);
  const [loading, setLoading]     = useState(false);
  const [filter, setFilter]       = useState({ status: "", priority: "", search: "" });
  const [showForm, setShowForm]   = useState(false);
  const [editTask, setEditTask]   = useState(null);
  const [form, setForm]           = useState({
    title: "", description: "", priority: "MEDIUM",
    category: "WORK", status: "TODO", dueDate: ""
  });

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get("/api/tasks");
      setTasks(res.data);
    } catch {
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const saved = localStorage.getItem("user");
    if (token && saved) {
      setUser(JSON.parse(saved));
      setScreen("app");
      fetchTasks();
    }
  }, [fetchTasks]);

  const handleAuth = async (mode) => {
    setAuthError("");
    if (!authForm.username || !authForm.password) {
      setAuthError("Please fill in all fields.");
      return;
    }
    try {
      const res = await api.post(`/api/auth/${mode}`, authForm);
      if (mode === "register") {
        setAuthError("Registered! Please sign in.");
        setScreen("login");
        return;
      }
      const u = { userId: res.data.userId, username: res.data.username };
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("user", JSON.stringify(u));
      setUser(u);
      setScreen("app");
      fetchTasks();
    } catch (e) {
      setAuthError(e.response?.data?.error || "Something went wrong.");
    }
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
    setTasks([]);
    setScreen("login");
    setAuthForm({ username: "", password: "" });
  };

  const openAdd = () => {
    setEditTask(null);
    setForm({ title: "", description: "", priority: "MEDIUM", category: "WORK", status: "TODO", dueDate: "" });
    setShowForm(true);
  };

  const openEdit = (task) => {
    setEditTask(task);
    setForm({
      title: task.title,
      description: task.description || "",
      priority: task.priority,
      category: task.category,
      status: task.status,
      dueDate: task.dueDate || ""
    });
    setShowForm(true);
  };

  const saveTask = async () => {
    if (!form.title.trim()) return;
    try {
      if (editTask) {
        const res = await api.put(`/api/tasks/${editTask.id}`, form);
        setTasks(tasks.map(t => t.id === editTask.id ? res.data : t));
      } else {
        const res = await api.post("/api/tasks", form);
        setTasks([res.data, ...tasks]);
      }
      setShowForm(false);
    } catch {
      alert("Failed to save task.");
    }
  };

  const deleteTask = async (id) => {
    if (!window.confirm("Delete this task?")) return;
    try {
      await api.delete(`/api/tasks/${id}`);
      setTasks(tasks.filter(t => t.id !== id));
    } catch {
      alert("Failed to delete.");
    }
  };

  const toggleDone = async (task) => {
    const newStatus = task.status === "DONE" ? "TODO" : "DONE";
    try {
      const res = await api.put(`/api/tasks/${task.id}`, { ...task, status: newStatus });
      setTasks(tasks.map(t => t.id === task.id ? res.data : t));
    } catch {
      setTasks(tasks.map(t => t.id === task.id ? { ...t, status: newStatus } : t));
    }
  };

  const filtered = tasks.filter(t => {
    if (filter.status && t.status !== filter.status) return false;
    if (filter.priority && t.priority !== filter.priority) return false;
    if (filter.search && !t.title.toLowerCase().includes(filter.search.toLowerCase())) return false;
    return true;
  });

  const stats = {
    total: tasks.length,
    done: tasks.filter(t => t.status === "DONE").length,
    inProgress: tasks.filter(t => t.status === "IN_PROGRESS").length,
    overdue: tasks.filter(t => t.dueDate && new Date(t.dueDate) < new Date() && t.status !== "DONE").length,
  };

  if (screen !== "app") {
    return (
      <div style={s.authPage}>
        <div style={s.authCard}>
          <div style={s.authLogo}>✦</div>
          <h1 style={s.authTitle}>TaskFlow</h1>
          <p style={s.authSub}>Stay organized. Stay ahead.</p>

          <div style={s.tabs}>
            {["login", "register"].map(m => (
              <button key={m} onClick={() => { setScreen(m); setAuthError(""); }}
                style={{ ...s.tab, ...(screen === m ? s.tabActive : {}) }}>
                {m === "login" ? "Sign In" : "Register"}
              </button>
            ))}
          </div>

          <input style={s.input} placeholder="Username"
            value={authForm.username}
            onChange={e => setAuthForm({ ...authForm, username: e.target.value })}
            onKeyDown={e => e.key === "Enter" && handleAuth(screen)} />

          <input style={s.input} placeholder="Password" type="password"
            value={authForm.password}
            onChange={e => setAuthForm({ ...authForm, password: e.target.value })}
            onKeyDown={e => e.key === "Enter" && handleAuth(screen)} />

          {authError && <p style={s.authError}>{authError}</p>}

          <button style={s.primaryBtn} onClick={() => handleAuth(screen)}>
            {screen === "login" ? "Sign In" : "Create Account"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={s.page}>
      <header style={s.header}>
        <span style={s.logo}>✦ TaskFlow</span>
        <div style={s.headerRight}>
          <span style={s.username}>👤 {user?.username}</span>
          <button style={s.logoutBtn} onClick={logout}>Logout</button>
        </div>
      </header>

      <div style={s.container}>
        <div style={s.statsRow}>
          {[
            { label: "Total",       value: stats.total,      color: "#6366f1" },
            { label: "Done",        value: stats.done,       color: "#22c55e" },
            { label: "In Progress", value: stats.inProgress, color: "#f59e0b" },
            { label: "Overdue",     value: stats.overdue,    color: "#ef4444" },
          ].map(c => (
            <div key={c.label} style={{ ...s.statCard, borderTop: `3px solid ${c.color}` }}>
              <div style={{ ...s.statNum, color: c.color }}>{c.value}</div>
              <div style={s.statLabel}>{c.label}</div>
            </div>
          ))}
        </div>

        <div style={s.toolbar}>
          <input style={s.searchInput} placeholder="🔍  Search tasks..."
            value={filter.search}
            onChange={e => setFilter({ ...filter, search: e.target.value })} />
          <select style={s.select} value={filter.status}
            onChange={e => setFilter({ ...filter, status: e.target.value })}>
            <option value="">All Status</option>
            {Object.entries(STATUS_META).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </select>
          <select style={s.select} value={filter.priority}
            onChange={e => setFilter({ ...filter, priority: e.target.value })}>
            <option value="">All Priority</option>
            {Object.entries(PRIORITY_META).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </select>
          <button style={s.primaryBtn} onClick={openAdd}>+ New Task</button>
        </div>

        {loading ? (
          <div style={s.empty}>Loading tasks...</div>
        ) : filtered.length === 0 ? (
          <div style={s.empty}>
            <div style={{ fontSize: "48px", marginBottom: "12px" }}>📋</div>
            <div>No tasks found. Add one!</div>
          </div>
        ) : (
          <div style={s.taskList}>
            {filtered.map(task => {
              const pm = PRIORITY_META[task.priority] || PRIORITY_META.MEDIUM;
              const sm = STATUS_META[task.status]    || STATUS_META.TODO;
              const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== "DONE";
              return (
                <div key={task.id} style={{ ...s.taskCard, opacity: task.status === "DONE" ? 0.65 : 1 }}>
                  <div style={s.taskLeft}>
                    <button
                      style={{ ...s.checkBtn, background: task.status === "DONE" ? "#22c55e" : "#e5e7eb" }}
                      onClick={() => toggleDone(task)}>
                      {task.status === "DONE" ? "✓" : ""}
                    </button>
                    <div style={s.taskInfo}>
                      <div style={{ ...s.taskTitle, textDecoration: task.status === "DONE" ? "line-through" : "none" }}>
                        {task.title}
                      </div>
                      {task.description && <div style={s.taskDesc}>{task.description}</div>}
                      <div style={s.taskMeta}>
                        <span style={{ ...s.badge, color: pm.color, background: pm.bg }}>{pm.label}</span>
                        <span style={{ ...s.badge, color: sm.color, background: sm.bg }}>{sm.label}</span>
                        {task.category && <span style={s.badgeGray}>{task.category}</span>}
                        {task.dueDate && (
                          <span style={{ ...s.badgeGray, color: isOverdue ? "#ef4444" : "#6b7280" }}>
                            {isOverdue ? "⚠ " : "📅 "}{task.dueDate}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div style={s.taskActions}>
                    <button style={s.editBtn}   onClick={() => openEdit(task)}>Edit</button>
                    <button style={s.deleteBtn} onClick={() => deleteTask(task.id)}>Delete</button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {showForm && (
        <div style={s.overlay} onClick={() => setShowForm(false)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <h2 style={s.modalTitle}>{editTask ? "Edit Task" : "New Task"}</h2>

            <label style={s.label}>Title *</label>
            <input style={s.input} placeholder="Task title"
              value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />

            <label style={s.label}>Description</label>
            <textarea style={{ ...s.input, height: "80px", resize: "vertical" }}
              placeholder="Optional description..."
              value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />

            <div style={s.formRow}>
              <div style={{ flex: 1 }}>
                <label style={s.label}>Priority</label>
                <select style={s.input} value={form.priority}
                  onChange={e => setForm({ ...form, priority: e.target.value })}>
                  {Object.entries(PRIORITY_META).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label style={s.label}>Status</label>
                <select style={s.input} value={form.status}
                  onChange={e => setForm({ ...form, status: e.target.value })}>
                  {Object.entries(STATUS_META).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
                </select>
              </div>
            </div>

            <div style={s.formRow}>
              <div style={{ flex: 1 }}>
                <label style={s.label}>Category</label>
                <select style={s.input} value={form.category}
                  onChange={e => setForm({ ...form, category: e.target.value })}>
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label style={s.label}>Due Date</label>
                <input style={s.input} type="date"
                  value={form.dueDate} onChange={e => setForm({ ...form, dueDate: e.target.value })} />
              </div>
            </div>

            <div style={s.modalBtns}>
              <button style={s.cancelBtn} onClick={() => setShowForm(false)}>Cancel</button>
              <button style={s.primaryBtn} onClick={saveTask}>
                {editTask ? "Save Changes" : "Add Task"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const s = {
  authPage: { minHeight: "100vh", background: "linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #1e1b4b 100%)", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "'Segoe UI', sans-serif" },
  authCard: { background: "white", borderRadius: "20px", padding: "40px", width: "100%", maxWidth: "400px", boxShadow: "0 25px 50px rgba(0,0,0,0.3)" },
  authLogo: { textAlign: "center", fontSize: "36px", color: "#6366f1", marginBottom: "8px" },
  authTitle: { textAlign: "center", margin: "0 0 4px", fontSize: "28px", fontWeight: "700", color: "#1e1b4b" },
  authSub: { textAlign: "center", color: "#9ca3af", marginBottom: "24px", fontSize: "14px" },
  tabs: { display: "flex", marginBottom: "20px", borderRadius: "10px", background: "#f3f4f6", padding: "4px" },
  tab: { flex: 1, padding: "8px", border: "none", background: "none", cursor: "pointer", borderRadius: "8px", fontSize: "14px", fontWeight: "500", color: "#6b7280" },
  tabActive: { background: "white", color: "#6366f1", boxShadow: "0 1px 4px rgba(0,0,0,0.1)" },
  input: { width: "100%", padding: "10px 14px", border: "1.5px solid #e5e7eb", borderRadius: "10px", fontSize: "14px", marginBottom: "12px", boxSizing: "border-box", outline: "none", fontFamily: "inherit" },
  authError: { color: "#ef4444", fontSize: "13px", marginBottom: "10px", textAlign: "center" },
  primaryBtn: { width: "100%", padding: "12px", background: "#6366f1", color: "white", border: "none", borderRadius: "10px", fontSize: "15px", fontWeight: "600", cursor: "pointer" },
  page: { minHeight: "100vh", background: "#f8fafc", fontFamily: "'Segoe UI', sans-serif" },
  header: { background: "white", borderBottom: "1px solid #e5e7eb", padding: "0 24px", height: "60px", display: "flex", alignItems: "center", justifyContent: "space-between", position: "sticky", top: 0, zIndex: 100 },
  logo: { fontWeight: "700", fontSize: "20px", color: "#6366f1" },
  headerRight: { display: "flex", alignItems: "center", gap: "16px" },
  username: { fontSize: "14px", color: "#6b7280" },
  logoutBtn: { padding: "6px 14px", background: "none", border: "1px solid #e5e7eb", borderRadius: "8px", cursor: "pointer", fontSize: "13px", color: "#6b7280" },
  container: { maxWidth: "900px", margin: "0 auto", padding: "24px 16px" },
  statsRow: { display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px", marginBottom: "20px" },
  statCard: { background: "white", borderRadius: "12px", padding: "16px", textAlign: "center", boxShadow: "0 1px 4px rgba(0,0,0,0.06)" },
  statNum: { fontSize: "28px", fontWeight: "700" },
  statLabel: { fontSize: "12px", color: "#9ca3af", marginTop: "2px" },
  toolbar: { display: "flex", gap: "10px", marginBottom: "16px", flexWrap: "wrap" },
  searchInput: { flex: 2, minWidth: "180px", padding: "10px 14px", border: "1.5px solid #e5e7eb", borderRadius: "10px", fontSize: "14px", outline: "none" },
  select: { flex: 1, minWidth: "120px", padding: "10px 14px", border: "1.5px solid #e5e7eb", borderRadius: "10px", fontSize: "14px", outline: "none", background: "white" },
  taskList: { display: "flex", flexDirection: "column", gap: "10px" },
  taskCard: { background: "white", borderRadius: "12px", padding: "16px", display: "flex", justifyContent: "space-between", alignItems: "center", boxShadow: "0 1px 4px rgba(0,0,0,0.06)", border: "1px solid #f1f5f9" },
  taskLeft: { display: "flex", alignItems: "flex-start", gap: "12px", flex: 1 },
  checkBtn: { width: "24px", height: "24px", borderRadius: "50%", border: "none", cursor: "pointer", color: "white", fontWeight: "700", fontSize: "13px", flexShrink: 0, marginTop: "2px" },
  taskInfo: { flex: 1 },
  taskTitle: { fontSize: "15px", fontWeight: "600", color: "#1f2937", marginBottom: "4px" },
  taskDesc: { fontSize: "13px", color: "#9ca3af", marginBottom: "6px" },
  taskMeta: { display: "flex", gap: "6px", flexWrap: "wrap" },
  badge: { fontSize: "11px", fontWeight: "600", padding: "2px 8px", borderRadius: "20px" },
  badgeGray: { fontSize: "11px", color: "#6b7280", background: "#f3f4f6", padding: "2px 8px", borderRadius: "20px" },
  taskActions: { display: "flex", gap: "8px", flexShrink: 0 },
  editBtn: { padding: "6px 14px", background: "#eff6ff", color: "#3b82f6", border: "none", borderRadius: "8px", cursor: "pointer", fontSize: "13px", fontWeight: "500" },
  deleteBtn: { padding: "6px 14px", background: "#fef2f2", color: "#ef4444", border: "none", borderRadius: "8px", cursor: "pointer", fontSize: "13px", fontWeight: "500" },
  empty: { textAlign: "center", color: "#9ca3af", padding: "60px 0", fontSize: "15px" },
  overlay: { position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 200, padding: "16px" },
  modal: { background: "white", borderRadius: "16px", padding: "28px", width: "100%", maxWidth: "500px", maxHeight: "90vh", overflowY: "auto" },
  modalTitle: { margin: "0 0 20px", fontSize: "20px", fontWeight: "700", color: "#1f2937" },
  label: { display: "block", fontSize: "13px", fontWeight: "600", color: "#374151", marginBottom: "4px" },
  formRow: { display: "flex", gap: "12px" },
  modalBtns: { display: "flex", gap: "10px", marginTop: "8px" },
  cancelBtn: { flex: 1, padding: "12px", background: "#f3f4f6", color: "#6b7280", border: "none", borderRadius: "10px", fontSize: "15px", fontWeight: "600", cursor: "pointer" },
};