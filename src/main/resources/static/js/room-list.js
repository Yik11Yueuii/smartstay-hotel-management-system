// 加载客房列表
function loadRooms() {
    const searchInput = document.getElementById('searchInput').value;
    const typeFilter = document.getElementById('typeFilter').value;

    let url = '/api/room/list?page=1&size=100&status=1';
    if (searchInput) url += `&roomName=${searchInput}`;
    if (typeFilter) url += `&roomType=${typeFilter}`;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                renderRooms(data.data.records);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('加载失败:', error);
            alert('网络错误,请检查后端是否启动');
        });
}

function renderRooms(rooms) {
    const html = rooms.map(r => `
        <div class="room-item">
            <div class="room-img">🏨</div>
            <div class="room-content">
                <div class="room-header">
                    <div>
                        <div class="room-name">${r.roomName} <span class="badge badge-info">${r.roomType}</span></div>
                        <div class="room-desc">房间号: ${r.roomNumber} | 床型: ${r.bedType}</div>
                        <div class="room-desc">${r.description || ''}</div>
                    </div>
                </div>
                <div class="room-footer">
                    <div>
                        <span class="room-price">¥${r.isPromotion ? r.promotionPrice : r.price}</span>
                        ${r.isPromotion ? `<span class="old-price">¥${r.price}</span>` : ''}
                        <small>/晚</small>
                    </div>
                    <div class="room-actions">
                        <button class="btn btn-primary" onclick="bookRoom(${r.id})">立即预订</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');

    document.getElementById('roomList').innerHTML = html || '<p>没有找到符合条件的客房</p>';
}

// 预订客房
function bookRoom(roomId) {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user) {
        alert('请先登录');
        location.href = '/pages/user/login.html';
        return;
    }
    location.href = `/pages/user/booking.html?roomId=${roomId}`;
}

// 搜索
function searchRooms() {
    loadRooms();
}

// 页面加载
document.addEventListener('DOMContentLoaded', function() {
    loadRooms();
});
