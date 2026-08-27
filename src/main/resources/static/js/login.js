const loginForm = document.getElementById('loginForm');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const usernameError = document.getElementById('usernameError');
const passwordError = document.getElementById('passwordError');

loginForm.addEventListener('submit', function(e) {
    e.preventDefault();

    usernameError.style.display = 'none';
    passwordError.style.display = 'none';

    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();

    let isValid = true;

    if (username === '') {
        usernameError.textContent = '请输入用户名';
        usernameError.style.display = 'block';
        isValid = false;
    }

    if (password === '') {
        passwordError.textContent = '请输入密码';
        passwordError.style.display = 'block';
        isValid = false;
    }

    if (!isValid) return;

    // 调用后端API登录
    fetch('/api/user/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('登录成功!');
                localStorage.setItem('user', JSON.stringify(data.data.user));
                localStorage.setItem('token', data.data.token);

                // 根据角色跳转
                if (data.data.user.role === 1) {
                    window.location.href = '/pages/admin/admin-dashboard.html';
                } else {
                    window.location.href = '/pages/user/index.html';
                }
            } else {
                passwordError.textContent = data.message;
                passwordError.style.display = 'block';
            }
        })
        .catch(error => {
            console.error('登录失败:', error);
            passwordError.textContent = '网络错误,请检查后端是否启动';
            passwordError.style.display = 'block';
        });
});
