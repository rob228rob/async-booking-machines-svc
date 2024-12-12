document.addEventListener('DOMContentLoaded', () => {
    loadUserInfo().then(r => console.log(r));
    loadUserReservations().then(r => console.log(r));
});

/**
 * Функция для загрузки информации о пользователе
 */
async function loadUserInfo() {
    try {
        const response = await fetch('/api/users/get/current', {
            method: 'GET'
        });
        if (response.ok) {
            const user = await response.json();
            displayUserInfo(user);
        } else {
            console.error('Не удалось получить информацию о пользователе.');
        }
    } catch (error) {
        console.error('Ошибка при получении информации о пользователе:', error);
    }
}

/**
 * Функция для отображения информации о пользователе
 * @param {Object} user - Объект пользователя
 */
function displayUserInfo(user) {
    const userInfoDiv = document.getElementById('user-info');
    const roles = Array.isArray(user.roles) ? user.roles.join(', ') : 'Не указано';
    userInfoDiv.innerHTML = `
        <h2>Личный кабинет</h2>
        <p><strong>Имя:</strong> ${user.first_name || 'Не указано'}</p>
        <p><strong>Фамилия:</strong> ${user.last_name || 'Не указано'}</p>
        <p><strong>Email:</strong> ${user.email}</p>
        <p><strong>Роль:</strong> ${roles}</p>
    `;
}

/**
 * Функция для загрузки бронирований пользователя
 */
async function loadUserReservations() {
    try {
        const response = await fetch('/api/reservations/current-user', {
            method: 'GET'
        });
        if (response.ok) {
            const rawReservations = await response.json();
            const transformedReservations = transformReservations(rawReservations);
            displayReservations(transformedReservations);
        } else {
            console.error('Не удалось получить бронирования пользователя.');
        }
    } catch (error) {
        console.error('Ошибка при получении бронирований пользователя:', error);
    }
}
/**
 * Функция для отображения бронирований в таблице
 * @param {Array} reservations - Массив объектов бронирований
 */
function displayReservations(reservations) {
    const reservationsBody = document.getElementById('reservations-body');
    const noReservationsDiv = document.getElementById('no-reservations');
    const reservationsTable = document.getElementById('reservations-table');
    reservationsBody.innerHTML = ''; // Очистка таблицы перед добавлением

    if (reservations.length === 0) {
        noReservationsDiv.style.display = 'block';
        reservationsTable.style.display = 'none';
        return;
    } else {
        noReservationsDiv.style.display = 'none';
        reservationsTable.style.display = 'table';
    }

    reservations.forEach(reservation => {
        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${reservation.reservationId}</td>
            <td>${reservation.machineName}</td>
            <td>${reservation.dormitoryName}</td>
            <td>${reservation.reservationTime}</td>
            <td>${reservation.status}</td>
            <td>
                <button class="delete-button" data-reservation-id="${reservation.reservationId}">Удалить</button>
            </td>
        `;
        reservationsBody.appendChild(row);
    });

    // обработчики событий для кнопок удаления бронирований
    document.querySelectorAll('.delete-button[data-reservation-id]').forEach(button => {
        button.addEventListener('click', handleDeleteReservation);
    });
}

/**
 * Обработчик удаления бронирования
 * @param {Event} event - Событие клика на кнопке удаления
 */
async function handleDeleteReservation(event) {
    const reservationId = event.target.getAttribute('data-reservation-id');
    if (!reservationId) return;

    // Подтверждение действия
    const confirmDelete = confirm('Вы уверены, что хотите удалить это бронирование? Это действие нельзя отменить.');
    if (!confirmDelete) return;

    try {
        const response = await fetch(`/api/reservations/del/${reservationId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok || response.status === 204) {
            showSuccessMessage('Бронирование успешно удалено!');
            // Обновление
            await loadUserReservations();
        } else if (response.status === 404) {
            showErrorMessage('Бронирование не найдено.');
        } else {
            const errorData = await response.json();
            console.error('Ошибка при удалении бронирования:', errorData);
            showErrorMessage(errorData.message || 'Не удалось удалить бронирование.');
        }
    } catch (error) {
        console.error('Ошибка при удалении бронирования:', error);
        showErrorMessage('Произошла ошибка при удалении бронирования.');
    }
}

/**
 * Функция для отображения сообщений об успехе
 * @param {string} message - Текст сообщения
 */
function showSuccessMessage(message) {
    const successMessage = document.getElementById('success-message');
    successMessage.textContent = message;
    successMessage.classList.remove('error');
    successMessage.classList.add('success');
    successMessage.style.display = 'block';

    // Скрыть сообщение через 5 секунд
    setTimeout(() => {
        successMessage.style.display = 'none';
    }, 5000);
}

/**
 * Функция для отображения сообщений об ошибке
 * @param {string} message - Текст сообщения
 */
function showErrorMessage(message) {
    const errorMessage = document.getElementById('error-message');
    errorMessage.textContent = message;
    errorMessage.classList.remove('success');
    errorMessage.classList.add('error');
    errorMessage.style.display = 'block';

    setTimeout(() => {
        errorMessage.style.display = 'none';
    }, 5000);
}

/**
 * Преобразует данные из ответа сервера в нужный формат
 * @param {Array} rawReservations - Исходные данные
 * @returns {Array} - Преобразованные данные
 */
function transformReservations(rawReservations) {
    const days = ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];

    return rawReservations.map(reservation => {
        const reservationDate = new Date(reservation.reservationDate);
        return {
            reservationId: reservation.reservationId,
            machineName: reservation.machineName,
            dormitoryName: reservation.dormitoryName,
            reservationTime: `${reservation.reservationDate}, ${reservation.reservationTime}`, // Добавляем день недели
            status: reservation.status
        };
    });
}

/**
 * Функция для форматирования временного слота
 * @param {Object} timeSlot - Объект временного слота
 * @returns {string} - Отформатированная строка временного слота
 */
function formatTimeSlot(timeSlot) {
    const days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
    const dayName = days[timeSlot.dayOfWeek - 1] || 'Неизвестно';
    return `${dayName}, ${formatTime(timeSlot.startTime)} - ${formatTime(timeSlot.endTime)}`;
}

/**
 * Функция для форматирования времени
 * @param {string} timeStr - Строка времени в формате "HH:MM:SS"
 * @returns {string} - Отформатированная строка времени "HH:MM"
 */
function formatTime(timeStr) {
    const time = new Date(`1970-01-01T${timeStr}Z`);
    return time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/**
 * Функция для форматирования даты
 * @param {string} dateStr - Строка даты в формате ISO
 * @returns {string} - Отформатированная строка даты "ДД.ММ.ГГГГ ЧЧ:ММ"
 */
function formatDate(dateStr) {
    const date = new Date(dateStr);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Месяцы начинаются с 0
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}.${month}.${year} ${hours}:${minutes}`;
}
