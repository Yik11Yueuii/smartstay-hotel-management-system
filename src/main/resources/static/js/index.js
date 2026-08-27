// 加载通知公告
function loadNotices() {
    fetch('/api/notice/show?page=1&size=4')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                renderNotices(data.data.records);
            }
        })
        .catch(error => {
            console.error('加载公告失败:', error);
            document.getElementById('noticeList').innerHTML = '<p>加载失败,请检查后端是否启动</p>';
        });
}

function renderNotices(notices) {
    const html = notices.map(n => `
        <div class="notice-item" onclick="alert('${n.content || n.title}')">
            <div class="notice-title">${n.title}</div>
            <div class="notice-time">${n.createTime}</div>
        </div>
    `).join('');

    document.getElementById('noticeList').innerHTML = html || '<p>暂无公告</p>';
}

// 加载推荐客房
function loadRooms() {
    fetch('/api/room/list?page=1&size=6&status=1')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                renderRooms(data.data.records);
            }
        })
        .catch(error => {
            console.error('加载客房失败:', error);
            document.getElementById('roomList').innerHTML = '<p>加载失败,请检查后端是否启动</p>';
        });
}

function renderRooms(rooms) {
    const html = rooms.map(r => `
        <div class="room-card" onclick="location.href='/pages/user/room-list.html'">
            <div class="room-img">🏨</div>
            <div class="room-info">
                <div class="room-name">${r.roomName}</div>
                <div class="room-type">${r.roomType}</div>
                <div class="room-price">
                    ¥${r.isPromotion ? r.promotionPrice : r.price}
                    <small>/晚</small>
                    ${r.isPromotion ? '<span class="promotion-tag">促销</span>' : ''}
                </div>
            </div>
        </div>
    `).join('');

    document.getElementById('roomList').innerHTML = html || '<p>暂无客房</p>';
}

// 页面加载
document.addEventListener('DOMContentLoaded', function() {
    loadNotices();
    loadRooms();
});
