// 检查登录状态
function checkLogin() {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userName = document.getElementById('userName');
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');

    if (user) {
        userName.textContent = user.nickname || user.username;
        loginBtn.style.display = 'none';
        logoutBtn.style.display = 'block';
    } else {
        userName.textContent = '未登录';
        loginBtn.style.display = 'block';
        logoutBtn.style.display = 'none';
    }
}

// 退出登录
document.addEventListener('DOMContentLoaded', function() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async function() {
            if (await AppDialog.confirm('确定要退出登录吗?')) {
                localStorage.removeItem('user');
                localStorage.removeItem('token');
                location.href = '/pages/user/index.html';
            }
        });
    }
    checkLogin();
});

// 格式化日期
function formatDate(dateStr) {
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

// 格式化时间
function formatDateTime(dateStr) {
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
