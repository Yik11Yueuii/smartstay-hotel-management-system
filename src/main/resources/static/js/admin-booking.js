// 检查管理员登录
function checkAdminLogin() {
    const admin = JSON.parse(localStorage.getItem('admin') || 'null');
    if (!admin || admin.role !== 1) {
        alert('请先登录管理员账号');
        location.href = '/pages/admin/admin-login.html';
        return;
    }
    document.getElementById('adminName').textContent = admin.nickname || admin.username;
}

// 退出登录
document.getElementById('logoutBtn')?.addEventListener('click', function() {
    if (confirm('确定要退出吗?')) {
        localStorage.removeItem('admin');
        location.href = '/pages/admin/admin-login.html';
    }
});

// 加载订单列表
function loadBookings() {
    const searchInput = document.getElementById('searchInput').value;
    const statusFilter = document.getElementById('statusFilter').value;

    let url = 'http://localhost:8080/api/booking/list?page=1&size=100';
    if (searchInput) url += `&orderNo=${searchInput}`;
    if (statusFilter) url += `&status=${statusFilter}`;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                renderBookings(data.data.records);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('加载失败:', error);
            alert('网络错误,请检查后端是否启动');
        });
}

// 渲染订单列表
function renderBookings(bookings) {
    const statusMap = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已入住', 4: '已退房' };
    const statusClass = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info' };

    const html = bookings.map(b => `
        <tr>
            <td>${b.orderNo}</td>
            <td>用户${b.userId}</td>
            <td>${b.roomName} (${b.roomNumber})</td>
            <td>${b.checkInDate}</td>
            <td>${b.checkOutDate}</td>
            <td>${b.days}天</td>
            <td><strong style="color:#f56c6c;">¥${b.totalAmount}</strong></td>
            <td><span class="badge badge-${statusClass[b.status]}">${statusMap[b.status]}</span></td>
            <td class="action-buttons">
                ${b.status === 0 ? `<button class="btn btn-primary btn-sm" onclick="confirmBooking(${b.id})">确认</button>` : ''}
                ${b.status === 1 ? `<button class="btn btn-primary btn-sm" onclick="checkIn(${b.id})">办理入住</button>` : ''}
                ${b.status === 3 ? `<button class="btn btn-primary btn-sm" onclick="checkOut(${b.id})">办理退房</button>` : ''}
            </td>
        </tr>
    `).join('');

    document.getElementById('bookingList').innerHTML = html || '<tr><td colspan="9" style="text-align:center;">暂无数据</td></tr>';
}

// 确认订单
function confirmBooking(id) {
    if (confirm('确定要确认此订单吗?')) {
        fetch(`http://localhost:8080/api/booking/confirm/${id}`, { method: 'PUT' })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    alert('确认成功!');
                    loadBookings();
                } else {
                    alert(data.message);
                }
            })
            .catch(error => {
                console.error('确认失败:', error);
                alert('网络错误');
            });
    }
}

// 办理入住
function checkIn(id) {
    const guestName = prompt('请输入客人姓名:');
    if (!guestName) return;

    const guestIdCard = prompt('请输入身份证号:');
    if (!guestIdCard) return;

    const deposit = prompt('请输入押金金额:', '200');
    if (!deposit) return;

    fetch('http://localhost:8080/api/booking/checkin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookingId: id, guestName, guestIdCard, deposit })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('入住成功!');
                loadBookings();
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('入住失败:', error);
            alert('网络错误');
        });
}

// 办理退房
function checkOut(id) {
    const depositReturn = prompt('请输入退还押金:', '200');
    if (depositReturn === null) return;

    const additionalCharges = prompt('请输入额外费用:', '0');
    if (additionalCharges === null) return;

    fetch('http://localhost:8080/api/booking/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookingId: id, depositReturn, additionalCharges, remark: '' })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('退房成功!');
                loadBookings();
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('退房失败:', error);
            alert('网络错误');
        });
}

// 页面加载
checkAdminLogin();
loadBookings();