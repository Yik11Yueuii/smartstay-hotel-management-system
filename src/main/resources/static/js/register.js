const registerForm = document.getElementById('registerForm');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const confirmPasswordInput = document.getElementById('confirmPassword');
const nicknameInput = document.getElementById('nickname');
const phoneInput = document.getElementById('phone');

const usernameError = document.getElementById('usernameError');
const passwordError = document.getElementById('passwordError');
const confirmPasswordError = document.getElementById('confirmPasswordError');
const phoneError = document.getElementById('phoneError');

registerForm.addEventListener('submit', function(e) {
    e.preventDefault();

    usernameError.style.display = 'none';
    passwordError.style.display = 'none';
    confirmPasswordError.style.display = 'none';
    phoneError.style.display = 'none';

    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();
    const confirmPassword = confirmPasswordInput.value.trim();
    const nickname = nicknameInput.value.trim();
    const phone = phoneInput.value.trim();

    let isValid = true;

    if (username === '') {
        usernameError.textContent = '请输入用户名';
        usernameError.style.display = 'block';
        isValid = false;
    } else if (username.length < 4 || username.length > 20) {
        usernameError.textContent = '用户名长度应为4-20个字符';
        usernameError.style.display = 'block';
        isValid = false;
    }

    if (password === '') {
        passwordError.textContent = '请输入密码';
        passwordError.style.display = 'block';
        isValid = false;
    } else if (password.length < 6 || password.length > 20) {
        passwordError.textContent = '密码长度应为6-20个字符';
        passwordError.style.display = 'block';
        isValid = false;
    }

    if (confirmPassword === '') {
        confirmPasswordError.textContent = '请确认密码';
        confirmPasswordError.style.display = 'block';
        isValid = false;
    } else if (password !== confirmPassword) {
        confirmPasswordError.textContent = '两次输入的密码不一致';
        confirmPasswordError.style.display = 'block';
        isValid = false;
    }

    if (phone !== '' && !/^1[3-9]\d{9}$/.test(phone)) {
        phoneError.textContent = '请输入正确的手机号';
        phoneError.style.display = 'block';
        isValid = false;
    }

    if (!isValid) return;

    // 调用后端API注册
    fetch('http://localhost:8080/api/user/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password, nickname, phone })
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('注册成功!请登录');
                window.location.href = '/pages/user/login.html';
            } else {
                usernameError.textContent = data.message;
                usernameError.style.display = 'block';
            }
        })
        .catch(error => {
            console.error('注册失败:', error);
            alert('网络错误,请检查后端是否启动');
        });
});