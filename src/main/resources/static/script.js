document.addEventListener('DOMContentLoaded', function() {
    // Ждем 3 секунды для загрузки reCAPTCHA
    setTimeout(checkCaptchaAvailability, 3000);

});

function checkCaptchaAvailability() {
    // Если reCAPTCHA не загрузилась, показываем математическую задачу
    if (typeof grecaptcha === 'undefined' ||
        typeof grecaptcha.getResponse !== 'function' ||
        !document.getElementById('recaptcha-container').querySelector('iframe')) {

        showMathCaptcha();
    }
}

function showMathCaptcha() {
    const mathCaptcha = document.getElementById('math-captcha');
    const recaptchaContainer = document.getElementById('recaptcha-container');

    // Скрываем неработающую reCAPTCHA
    recaptchaContainer.style.display = 'none';

    // Показываем математическую задачу
    const { problem, answer } = generateMathProblem();
    document.getElementById('math-problem').textContent = problem;
    mathCaptcha.style.display = 'block';
    mathCaptcha.dataset.answer = answer;
}

function generateMathProblem() {
    const num1 = Math.floor(Math.random() * 10) + 1;
    const num2 = Math.floor(Math.random() * 10) + 1;
    const operators = ['+', '-', '*'];
    const operator = operators[Math.floor(Math.random() * operators.length)];

    let problem, answer;
    switch(operator) {
        case '+':
            problem = `${num1} + ${num2}`;
            answer = num1 + num2;
            break;
        case '-':
            problem = `${num1} - ${num2}`;
            answer = num1 - num2;
            break;
        case '*':
            problem = `${num1} * ${num2}`;
            answer = num1 * num2;
            break;
    }

    return { problem, answer };
}
// Переключение между вкладками
function openTab(tabName) {
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });
    document.getElementById(tabName).classList.add('active');
}

// Регистрация с reCAPTCHA v2
async function register() {
    const username = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const errorElement = document.getElementById('register-error');
    errorElement.textContent = '';

    // Базовые проверки полей
    if (!username || !email || !password) {
        errorElement.textContent = "Пожалуйста, заполните все поля";
        return;
    }

    const mathCaptcha = document.getElementById('math-captcha');
    let verificationData = {};

    try {
        if (mathCaptcha.style.display !== 'none') {
            // Проверка математической капчи
            const userAnswer = document.getElementById('math-answer').value.trim();
            const correctAnswer = mathCaptcha.dataset.answer;

            // Проверяем, что ответ не пустой
            if (!userAnswer) {
                errorElement.textContent = "Пожалуйста, введите ответ на задачу";
                document.getElementById('math-answer').focus();
                return;
            }

            // Проверяем, что ответ - число
            if (isNaN(userAnswer)) {
                errorElement.textContent = "Ответ должен быть числом";
                document.getElementById('math-answer').value = '';
                document.getElementById('math-answer').focus();
                return;
            }

            // Проверяем правильность ответа
            if (parseInt(userAnswer) !== parseInt(correctAnswer)) {
                errorElement.textContent = "Неправильный ответ. Попробуйте еще раз.";
                const { problem, answer } = generateMathProblem();
                document.getElementById('math-problem').textContent = problem;
                mathCaptcha.dataset.answer = answer;
                document.getElementById('math-answer').value = '';
                document.getElementById('math-answer').focus();
                return;
            }

            verificationData = {
                verificationType: 'math',
                mathAnswer: correctAnswer,
                mathProblem: document.getElementById('math-problem').textContent
            };
        } else {
            // Проверка reCAPTCHA
            if (typeof grecaptcha === 'undefined') {
                errorElement.textContent = "Ошибка загрузки системы проверки. Пожалуйста, обновите страницу или используйте другой браузер.";
                return;
            }

            const captchaResponse = grecaptcha.getResponse();
            if (!captchaResponse) {
                errorElement.textContent = "Пожалуйста, подтвердите, что вы не робот";
                return;
            }

            verificationData = {
                verificationType: 'recaptcha',
                recaptchaToken: captchaResponse
            };
        }

        // Подготовка данных для отправки
        const requestData = {
            rqid: 123456789,
            registrationData: {
                username: username,
                email: email,
                password: password,
                ...verificationData
            }
        };


        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestData)
        });

        // Обработка ответа сервера
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(
                errorData.message ||
                errorData.error ||
                `Ошибка сервера: ${response.status} ${response.statusText}`
            );
        }

        // Успешная регистрация
        alert("Регистрация успешна! Теперь вы можете войти.");
        openTab('login');

        // Сброс формы
        resetForm();

    } catch (error) {
        console.error('Ошибка регистрации:', error);

        // Улучшенный вывод ошибок
        if (error.message.includes('Failed to fetch')) {
            errorElement.textContent = "Ошибка соединения с сервером. Проверьте интернет-соединение.";
        } else {
            errorElement.textContent = error.message || "Произошла неизвестная ошибка при регистрации";
        }

        // Сброс капчи при ошибке
        resetCaptcha();
    }
}

// Сброс формы
function resetForm() {
    document.getElementById('math-captcha').style.display = 'none';
    clearRegistrationForm();
    resetCaptcha();
}

// Сброс капчи
function resetCaptcha() {
    const mathCaptcha = document.getElementById('math-captcha');
    if (mathCaptcha.style.display !== 'none') {
        const { problem, answer } = generateMathProblem();
        document.getElementById('math-problem').textContent = problem;
        mathCaptcha.dataset.answer = answer;
    } else if (typeof grecaptcha !== 'undefined') {
        grecaptcha.reset();
        recaptchaContainer.style.display = 'none';
    }
}

// Вход
async function login() {
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    const errorElement = document.getElementById('login-error');
    errorElement.textContent = '';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                rqid: 123456789,
                loginData: {
                    usernameOrEmail: username,
                    password: password
                }
            })
        });

        const data = await response.json();

        if (!response.ok) {
            alert(`Ошибка входа`)
            throw new Error(data.message || "Ошибка входа");
        }

        // Выводим информацию о пользователе
        const userInfo = `Вход выполнен успешно!\n\nДанные пользователя:\n` +
            `ID: ${data.response.user.id}\n` +
            `Логин: ${data.response.user.username}\n` +
            `Email: ${data.response.user.email}\n` +
            `Дата регистрации: ${new Date(data.response.user.createdAt).toLocaleString()}`;

        alert(userInfo);
        clearLoginForm();
    } catch (error) {
        errorElement.textContent = error.message;
    }
}

function clearRegistrationForm() {
    document.getElementById('reg-username').value = '';
    document.getElementById('reg-email').value = '';
    document.getElementById('reg-password').value = '';
}

function clearLoginForm() {
    document.getElementById('login-username').value = '';
    document.getElementById('login-password').value = '';
}