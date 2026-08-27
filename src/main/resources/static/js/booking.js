let currentRoom = null;

// 获取URL参数
function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// 加载客房信息
function loadRoomInfo() {
    const roomId = getQueryParam('roomId');

    fetch(`/api/room/${roomId}`)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                currentRoom = data.data;
                const price = currentRoom.isPromotion ? currentRoom.promotionPrice : currentRoom.price;
                document.getElementById('roomInfo').innerHTML = `
                    <strong>${currentRoom.roomName}</strong> (房间号: ${currentRoom.roomNumber})<br>
                    价格: ¥${price}/晚 ${currentRoom.isPromotion ? '<span class="badge badge-danger">促销</span>' : ''}
                `;
                document.getElementById('unitPrice').textContent = price;
            }
        })
        .catch(error => {
            console.error('加载失败:', error);
            alert('加载客房信息失败');
        });
}

// 计算价格
function calculatePrice() {
    const checkIn = document.getElementById('checkInDate').value;
    const checkOut = document.getElementById('checkOutDate').value;

    if (checkIn && checkOut && currentRoom) {
        const date1 = new Date(checkIn);
        const date2 = new Date(checkOut);
        const days = Math.ceil((date2 - date1) / (1000 * 60 * 60 * 24));

        if (days > 0) {
            const price = currentRoom.isPromotion ? currentRoom.promotionPrice : currentRoom.price;
            const total = days * price;

            document.getElementById('days').textContent = days;
            document.getElementById('totalPrice').textContent = total;
        }
    }
}

// 监听日期变化
document.getElementById('checkInDate')?.addEventListener('change', calculatePrice);
document.getElementById('checkOutDate')?.addEventListener('change', calculatePrice);

// 设置最小日期为今天
const today = new Date().toISOString().split('T')[0];
document.getElementById('checkInDate').min = today;
document.getElementById('checkOutDate').min = today;

// 提交预订
document.getElementById('bookingForm')?.addEventListener('submit', function(e) {
    e.preventDefault();

    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user) {
        alert('请先登录');
        location.href = '/pages/user/login.html';
        return;
    }

    const checkIn = document.getElementById('checkInDate').value;
    const checkOut = document.getElementById('checkOutDate').value;
    const contactName = document.getElementById('contactName').value;
    const contactPhone = document.getElementById('contactPhone').value;
    const remark = document.getElementById('remark').value;
    const days = parseInt(document.getElementById('days').textContent);
    const total = parseFloat(document.getElementById('totalPrice').textContent);

    if (days <= 0) {
        alert('请选择有效的入住和退房日期');
        return;
    }

    const bookingData = {
        userId: user.id,
        roomId: currentRoom.id,
        roomName: currentRoom.roomName,
        roomNumber: currentRoom.roomNumber,
        checkInDate: checkIn,
        checkOutDate: checkOut,
        days: days,
        price: currentRoom.isPromotion ? currentRoom.promotionPrice : currentRoom.price,
        totalAmount: total,
        contactName: contactName,
        contactPhone: contactPhone,
        remark: remark
    };

    // 调用后端API创建订单
    fetch('/api/booking/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(bookingData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('预订成功!');
                location.href = '/pages/user/user-center.html';
            } else {
                alert('预订失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('预订失败:', error);
            alert('网络错误,请检查后端是否启动');
        });
});

// 页面加载
loadRoomInfo();
