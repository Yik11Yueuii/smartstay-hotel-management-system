// 切换标签页
function showTab(tabName) {
    // 隐藏所有内容
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.sidebar-menu li').forEach(li => {
        li.classList.remove('active');
    });

    // 显示选中的内容
    document.getElementById(tabName).classList.add('active');
    event.target.classList.add('active');

    // 加载对应数据
    if (tabName === 'profile') loadProfile();
    if (tabName === 'bookings') loadBookings();
    if (tabName === 'feedback') loadFeedback();
}

// 加载个人资料
function loadProfile() {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (user) {
        document.getElementById('username').value = user.username;
        document.getElementById('nickname').value = user.nickname || '';
        document.getElementById('phone').value = user.phone || '';
    }
}

// 保存个人资料
document.getElementById('profileForm')?.addEventListener('submit', function(e) {
    e.preventDefault();

    const user = JSON.parse(localStorage.getItem('user') || 'null');
    user.nickname = document.getElementById('nickname').value;
    user.phone = document.getElementById('phone').value;

    // TODO: 替换为真实API
    localStorage.setItem('user', JSON.stringify(user));
    alert('保存成功!');
    checkLogin();
});

// 修改密码
document.getElementById('passwordForm')?.addEventListener('submit', function(e) {
    e.preventDefault();

    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (newPassword !== confirmPassword) {
        alert('两次输入的密码不一致');
        return;
    }

    // TODO: 替换为真实API
    alert('密码修改成功! (模拟)');
    this.reset();
});

// 加载预订列表
// 加载预订列表
function loadBookings() {
    // 调用后端API
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user) {
        alert('请先登录');
        location.href = '/pages/user/login.html';
        return;
    }

    fetch(`http://localhost:8080/api/booking/list?userId=${user.id}`, {
        headers: authenticatedHeaders(false)
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                // 过滤掉已取消的订单
                const activeBookings = data.data.records.filter(b => b.status !== 2);
                renderUserBookings(activeBookings);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('加载失败:', error);
            // 模拟数据
            const bookings = [
                { id: 1, orderNo: 'ORD202401250001', roomName: '豪华单人间A', checkInDate: '2024-01-28', checkOutDate: '2024-01-30', totalAmount: 576, status: 0 },
                { id: 2, orderNo: 'ORD202401250002', roomName: '标准双人间B', checkInDate: '2024-02-01', checkOutDate: '2024-02-03', totalAmount: 656, status: 1 }
            ];
            // 只显示未取消的
            const activeBookings = bookings.filter(b => b.status !== 2);
            renderUserBookings(activeBookings);
        });
}

function renderUserBookings(bookings) {
    const statusMap = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已入住', 4: '已退房' };
    const statusClass = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info' };

    const html = bookings.map(b => `
        <div class="order-item">
            <div class="order-header">
                <span>订单号: ${b.orderNo}</span>
                <span class="badge badge-${statusClass[b.status]}">${statusMap[b.status]}</span>
            </div>
            <p>客房: ${b.roomName}</p>
            <p>入住: ${b.checkInDate} ~ ${b.checkOutDate}</p>
            <p>总价: <strong style="color:#f56c6c;">¥${b.totalAmount}</strong></p>
            ${b.status === 0 ? '<button class="btn btn-secondary" onclick="cancelBooking(' + b.id + ')">取消预订</button>' : ''}
        </div>
    `).join('');

    document.getElementById('bookingList').innerHTML = html || '<p>暂无预订记录</p>';
}
// 取消预订
function cancelBooking(id) {
    if (confirm('确定要取消此预订吗?')) {
        fetch(`/api/booking/cancel/${id}`, {
            method: 'PUT',
            headers: authenticatedHeaders(false)
        })
            .then(response => response.json())
            .then(data => {
                if (data.code !== 200) throw new Error(data.message || '取消失败');
                alert('取消成功!');
                loadBookings();
            })
            .catch(error => alert(error.message));
    }
}

// 显示反馈表单
function showFeedbackForm() {
    document.getElementById('feedbackForm').style.display = 'block';
}

function hideFeedbackForm() {
    document.getElementById('feedbackForm').style.display = 'none';
    document.getElementById('newFeedbackForm').reset();
}

// 提交反馈
document.getElementById('newFeedbackForm')?.addEventListener('submit', function(e) {
    e.preventDefault();

    const title = document.getElementById('feedbackTitle').value;
    const content = document.getElementById('feedbackContent').value;
    const type = document.getElementById('feedbackType').value;

    fetch('/api/feedback/create', {
        method: 'POST',
        headers: authenticatedHeaders(true),
        body: JSON.stringify({ title, content, type: parseInt(type) })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code !== 200) throw new Error(data.message || '提交失败');
            alert('反馈提交成功!');
            hideFeedbackForm();
            loadFeedback();
        })
        .catch(error => alert(error.message));
});

// 加载反馈列表
function loadFeedback() {
    fetch('/api/feedback/list?page=1&size=100', { headers: authenticatedHeaders(false) })
        .then(response => response.json())
        .then(data => {
            if (data.code !== 200) throw new Error(data.message || '加载失败');
            renderFeedback(data.data.records);
        })
        .catch(error => {
            document.getElementById('feedbackList').innerHTML = `<p>${error.message}</p>`;
        });
}

function renderFeedback(feedbacks) {

    const typeMap = { 0: '建议', 1: '投诉', 2: '表扬' };
    const statusMap = { 0: '待处理', 1: '已处理' };

    const html = feedbacks.map(f => `
        <div class="order-item">
            <div class="order-header">
                <span><strong>${f.title}</strong> <span class="badge badge-info">${typeMap[f.type]}</span></span>
                <span class="badge badge-${f.status === 1 ? 'success' : 'warning'}">${statusMap[f.status]}</span>
            </div>
            <p>${f.content}</p>
            <p style="font-size:12px; color:#999;">${f.createTime}</p>
            ${f.reply ? `<div style="background:#f9f9f9; padding:10px; border-radius:5px; margin-top:10px;">
                <strong>回复:</strong> ${f.reply}
            </div>` : ''}
        </div>
    `).join('');

    document.getElementById('feedbackList').innerHTML = html || '<p>暂无反馈记录</p>';
}

function authenticatedHeaders(json) {
    const headers = { 'Authorization': 'Bearer ' + (localStorage.getItem('token') || '') };
    if (json) headers['Content-Type'] = 'application/json';
    return headers;
}

// 页面加载
document.addEventListener('DOMContentLoaded', function() {
    loadProfile();
});
