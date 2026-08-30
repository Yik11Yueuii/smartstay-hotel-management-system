function authHeaders(json) {
    const headers = {'Authorization': 'Bearer ' + (localStorage.getItem('token') || '')};
    if (json) headers['Content-Type'] = 'application/json';
    return headers;
}

function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>'"]/g, character => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[character]));
}

function formatTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : '-';
}

function checkAdminLogin() {
    const admin = JSON.parse(localStorage.getItem('admin') || 'null');
    if (!admin || admin.role !== 1) {
        alert('请先登录管理员账号');
        location.href = '/pages/admin/admin-login.html';
        return false;
    }
    document.getElementById('adminName').textContent = admin.nickname || admin.username;
    return true;
}

async function loadOverview() {
    try {
        const response = await fetch('/api/operations/overview', {headers: authHeaders(false)});
        const result = await response.json();
        if (result.code !== 200) throw new Error(result.message || '运营数据加载失败');
        const overview = result.data;
        ['pendingTasks', 'inProgressTasks', 'overdueTasks', 'openReminders'].forEach(key => {
            document.getElementById(key).textContent = overview[key] || 0;
        });
        renderReminders(overview.reminders || []);
        renderTasks(overview.tasks || []);
    } catch (error) {
        document.getElementById('reminderList').innerHTML = '<div class="empty-state">' + escapeHtml(error.message) + '</div>';
        document.getElementById('taskList').innerHTML = '<tr><td colspan="7" class="empty-state">' + escapeHtml(error.message) + '</td></tr>';
    }
}

function renderReminders(reminders) {
    const container = document.getElementById('reminderList');
    if (!reminders.length) {
        container.innerHTML = '<div class="empty-state success-empty">✓ 当前没有待处理运营风险</div>';
        return;
    }
    container.innerHTML = reminders.map(reminder => '<article class="reminder-card level-' + reminder.level + '">'
        + '<div class="reminder-icon">' + (reminder.level === 2 ? '!' : 'i') + '</div>'
        + '<div class="reminder-content"><div class="reminder-title">' + escapeHtml(reminder.title) + '</div>'
        + '<p>' + escapeHtml(reminder.content) + '</p><small>触发时间：' + formatTime(reminder.triggerTime) + '</small></div>'
        + '<button class="btn btn-secondary btn-sm" onclick="resolveReminder(' + reminder.id + ')">标记已处理</button>'
        + '</article>').join('');
}

function renderTasks(tasks) {
    const statusMap = {0: '待处理', 1: '清洁中', 2: '已完成', 3: '已取消'};
    const statusClass = {0: 'warning', 1: 'info', 2: 'success', 3: 'danger'};
    if (!tasks.length) {
        document.getElementById('taskList').innerHTML = '<tr><td colspan="7" class="empty-state">暂无清洁任务</td></tr>';
        return;
    }
    document.getElementById('taskList').innerHTML = tasks.map(task => {
        let actions = '';
        if (task.status === 0) {
            actions = '<button class="btn btn-primary btn-sm" onclick="startTask(' + task.id + ')">开始清洁</button>';
        } else if (task.status === 1) {
            actions = '<button class="btn btn-primary btn-sm" onclick="completeTask(' + task.id + ')">完成清洁</button>';
        }
        const overdue = (task.status === 0 || task.status === 1) && task.dueTime && new Date(task.dueTime) < new Date();
        return '<tr class="' + (overdue ? 'overdue-row' : '') + '"><td>' + escapeHtml(task.taskNo) + '</td>'
            + '<td><strong>' + escapeHtml(task.roomNumber) + '</strong></td>'
            + '<td><span class="priority priority-' + task.priority + '">' + (task.priority === 2 ? '紧急' : '普通') + '</span></td>'
            + '<td><span class="badge badge-' + statusClass[task.status] + '">' + statusMap[task.status] + '</span></td>'
            + '<td>' + escapeHtml(task.assignee || '待分配') + '</td>'
            + '<td>' + formatTime(task.dueTime) + (overdue ? '<em class="overdue-label"> 已逾期</em>' : '') + '</td>'
            + '<td class="action-buttons">' + actions + '</td></tr>';
    }).join('');
}

async function request(url, method, body) {
    const response = await fetch(url, {
        method,
        headers: authHeaders(body != null),
        body: body == null ? undefined : JSON.stringify(body)
    });
    const result = await response.json();
    if (result.code !== 200) throw new Error(result.message || '操作失败');
    return result;
}

async function startTask(id) {
    const assignee = await AppDialog.prompt('请输入保洁负责人', '保洁组', '分配清洁任务');
    if (assignee == null) return;
    try {
        await request('/api/operations/tasks/' + id + '/start', 'PUT', {assignee});
        await loadOverview();
    } catch (error) {
        alert(error.message);
    }
}

async function completeTask(id) {
    if (!await AppDialog.confirm('确认房间已完成清洁并通过检查？完成后将自动恢复房态。', '完成清洁')) return;
    try {
        await request('/api/operations/tasks/' + id + '/complete', 'PUT');
        await AppDialog.alert('清洁已完成，房态与关联提醒已自动更新。', '闭环完成');
        await loadOverview();
    } catch (error) {
        alert(error.message);
    }
}

async function resolveReminder(id) {
    try {
        await request('/api/operations/reminders/' + id + '/resolve', 'PUT');
        await loadOverview();
    } catch (error) {
        alert(error.message);
    }
}

document.getElementById('scanBtn').addEventListener('click', async function() {
    this.disabled = true;
    try {
        const result = await request('/api/operations/reminders/scan', 'POST');
        await AppDialog.alert('风险扫描完成，新增 ' + result.data.created + ' 条提醒。', '扫描完成');
        await loadOverview();
    } catch (error) {
        alert(error.message);
    } finally {
        this.disabled = false;
    }
});

document.getElementById('logoutBtn').addEventListener('click', async function() {
    if (await AppDialog.confirm('确定要退出吗？', '退出后台')) {
        localStorage.removeItem('admin');
        localStorage.removeItem('token');
        location.href = '/pages/admin/admin-login.html';
    }
});

if (checkAdminLogin()) {
    loadOverview();
    setInterval(loadOverview, 30000);
}
