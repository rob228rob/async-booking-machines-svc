let selectedMachineId = null;
let pollingInterval = 150; // Интервал в миллисекундах
let maxAttempts = 5; // Максимальное количество попыток

document.addEventListener('DOMContentLoaded', () => {
    loadMachines().then(() => console.log("machines loaded"));
});

async function loadMachines() {
    try {
        const response = await fetch('/api/machines/get-all', {
            method: 'GET',
            credentials: 'include',
        });
        if (response.ok) {
            const data = await response.json();
            displayMachinesList(data);
        } else {
            console.error('Не удалось получить список машин.');
        }
    } catch (error) {
        console.error('Ошибка при получении списка машин:', error);
    }
}

function displayMachinesList(machines) {
    const machinesList = document.getElementById('machines-list');
    machinesList.innerHTML = '';

    machines.forEach(machine => {
        const machineCard = document.createElement('div');
        machineCard.classList.add('machine-card');
        machineCard.innerHTML = `
                <h3>${machine.name}</h3>
                <p><strong>Общежитие:</strong> ${machine.dormitoryName || 'Неизвестно'}</p>
                <p><strong>Тип машинки:</strong> ${machine.machineType || 'Неизвестно'}</p>
            `;

        machineCard.addEventListener('click', () => {
            selectedMachineId = machine.id;
            document.getElementById('slots-controls').style.display = 'flex';
            document.getElementById('machines-container').innerHTML = '';
        });

        machinesList.appendChild(machineCard);
    });

    const loadSlotsButton = document.getElementById('load-slots-button');
    loadSlotsButton.addEventListener('click', loadMachineTimeSlots);
}

async function loadMachineTimeSlots() {
    if (!selectedMachineId) {
        alert('Сначала выберите машинку');
        return;
    }

    const startDateInput = document.getElementById('start-date');
    const weeksSelect = document.getElementById('weeks');

    const startDate = startDateInput.value;
    const weeks = parseInt(weeksSelect.value);

    if (!startDate) {
        alert('Пожалуйста, выберите начальную дату');
        return;
    }

    // Сохраняем выбранную дату и количество недель для дальнейшего расчёта resDate
    loadMachineTimeSlots.startDate = new Date(startDate);
    loadMachineTimeSlots.weeks = weeks;

    const url = `/api/machine-slots/get?machineId=${selectedMachineId}&startDate=${startDate}&weeks=${weeks}`;
    try {
        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include',
        });
        if (response.ok) {
            const data = await response.json();
            displayGeneratedSlots(data.slots);
        } else {
            console.error('Не удалось получить слоты для машинки.');
        }
    } catch (error) {
        console.error('Ошибка при получении слотов для машинки:', error);
    }
}

function displayGeneratedSlots(machines) {
    const machinesContainer = document.getElementById('machines-container');
    machinesContainer.innerHTML = '';

    const { startDate, weeks } = loadMachineTimeSlots;

    // Генерируем все даты для выбранного периода
    const allDates = [];
    const start = new Date(startDate);
    // Устанавливаем начало недели на понедельник
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7));

    for (let week = 0; week < weeks; week++) {
        for (let day = 1; day <= 7; day++) { // 1=Понедельник, 7=Воскресенье
            const date = new Date(start);
            date.setDate(start.getDate() + week * 7 + (day - 1));
            allDates.push({
                dayOfWeek: day,
                date: date.toISOString().split('T')[0] // Формат YYYY-MM-DD
            });
        }
    }

    // Создаём карту для быстрого доступа к датам по dayOfWeek и неделе
    const dateMap = {};
    allDates.forEach((item, index) => {
        const weekNumber = Math.floor(index / 7) + 1;
        dateMap[`${item.dayOfWeek}-${weekNumber}`] = item.date;
    });

    machines.forEach((machine) => {
        machine.timeSlots.forEach((slot, index) => {
            // Определяем номер недели для текущего слота
            const weekNumber = Math.floor(index / machine.timeSlots.length * weeks) + 1;
            const resDate = dateMap[`${slot.dayOfWeek}-${weekNumber}`] || '';

            if (!resDate) {
                console.error(`Не удалось определить дату для dayOfWeek: ${slot.dayOfWeek}, weekNumber: ${weekNumber}`);
                return;
            }

            // Создаём карточку для машины, если ещё не создана
            let machineCard = machinesContainer.querySelector(`[data-machine-id="${machine.machineId}"]`);
            if (!machineCard) {
                machineCard = document.createElement('div');
                machineCard.classList.add('machine-card');
                machineCard.setAttribute('data-machine-id', machine.machineId);
                machineCard.innerHTML = `
                    <h3>${machine.machineName}</h3>
                    <p><strong>Общежитие:</strong> ${machine.dormitoryName}</p>
                    <ul class="time-slots"></ul>
                `;
                machinesContainer.appendChild(machineCard);
            }

            const timeSlotsList = machineCard.querySelector('.time-slots');

            const timeSlotItem = document.createElement('li');
            timeSlotItem.classList.add('time-slot', slot.available ? 'available' : 'booked');
            timeSlotItem.innerHTML = `
                <strong>${getDayOfWeekName(slot.dayOfWeek)} (${resDate})</strong>:
                <span>${slot.startTime} - ${slot.endTime}</span>
                ${slot.available
                ? `<button class="book"
                                data-machine-id="${machine.machineId}"
                                data-res-date="${resDate}"
                                data-start-time="${slot.startTime}"
                                data-end-time="${slot.endTime}">
                            Забронировать
                       </button>`
                : `<button class="booked" disabled>Забронировано</button>`}
            `;
            timeSlotsList.appendChild(timeSlotItem);
        });
    });

    // Добавляем обработчики событий для кнопок бронирования
    document.querySelectorAll('.book').forEach(button => {
        button.addEventListener('click', handleBooking);
    });
}

/**
 * Функция для обработки бронирования асинхронно с поллингом статуса
 * @param {Event} event - Событие клика на кнопке бронирования
 */
async function handleBooking(event) {
    const machineId = event.target.getAttribute('data-machine-id');
    const resDate = event.target.getAttribute('data-res-date');
    const startTime = event.target.getAttribute('data-start-time');
    const endTime = event.target.getAttribute('data-end-time');

    if (!machineId || !resDate || !startTime || !endTime) {
        console.error('Недостаточно данных для бронирования.');
        return;
    }

    // if (!confirm('Вы уверены, что хотите забронировать этот слот?')) {
    //     return;
    // }

    const reservationRequest = {
        machineId: machineId,
        resDate: resDate,
        startTime: startTime,
        endTime: endTime
    };

    try {
        const response = await fetch('/api/reservations/book', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify(reservationRequest),
        });

        if (response.status === 202) { // HTTP 202 Accepted
            const statusUrl = response.headers.get('Location');
            const reservationId = extractReservationIdFromUrl(statusUrl);
            showSuccessMessage('Бронирование инициировано. Ожидайте обновления статуса.');

            // Начинаем поллинг статуса
            pollReservationStatus(reservationId, statusUrl);
        } else if (response.status === 400) {
            alert('Выбрана невалидная дата, в прошлое нельзя бронироваться')
        }
        else {
            const errorData = await response.json();
            alert(`Ошибка при бронировании: ${errorData.message || 'Неизвестная ошибка.'}`);
        }
    } catch (error) {
        console.error('Ошибка при бронировании:', error);
        alert('Произошла ошибка при бронировании. Пожалуйста, попробуйте позже.');
    } finally {
        //hideSpinner();
    }
}

/**
 * Функция для извлечения reservationId из URL статуса
 * @param {string} statusUrl - URL статуса
 * @returns {string} - reservationId
 */
function extractReservationIdFromUrl(statusUrl) {
    const parts = statusUrl.split('/');
    return parts[parts.length - 1];
}

/**
 * Функция для поллинга статуса бронирования с использованием SweetAlert2
 * @param {string} reservationId - ID бронирования
 * @param {string} statusUrl - URL для проверки статуса
 */
function pollReservationStatus(reservationId, statusUrl) {

    let attempts = 0;

    console.log(`Начало поллинга статуса бронирования ID: ${reservationId}`);

    const intervalId = setInterval(async () => {
        console.log(`Проверка статуса бронирования ID: ${reservationId}, попытка ${attempts + 1}`);

        try {
            const response = await fetch(statusUrl, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (response.ok) {
                const statusText = await response.text();
                console.log(`Статус бронирования ID: ${reservationId} - ${statusText}`);

                if (statusText === 'Operation completed successfully') {
                    console.log({
                        title: 'Бронирование завершено',
                        text: 'Ваше бронирование успешно завершено!',
                        icon: 'success',
                        timer: 5000,
                        showConfirmButton: false
                    });
                    clearInterval(intervalId);
                    await loadMachineTimeSlots(); // Обновление списка слотов после успешного бронирования
                } else if (statusText.startsWith('Operation failed')) {
                    console.log({
                        title: 'Ошибка бронирования',
                        text: `Бронирование не удалось: ${statusText}`,
                        icon: 'error',
                        timer: 5000,
                        showConfirmButton: false
                    });
                    alert(`Бронирование не удалось: ${statusText}`)
                    clearInterval(intervalId);
                } else {
                    console.log(`Текущее состояние бронирования (${reservationId}): ${statusText}`);
                }
            } else if (response.status === 404) {
                console.error('Статус бронирования не найден.');
                console.log({
                    title: 'Ошибка',
                    text: 'Статус бронирования не найден.',
                    icon: 'error',
                    confirmButtonText: 'Ок'
                });
                alert(`Статус бронирования не найден`)
                clearInterval(intervalId);
            } else {
                const errorData = await response.json();
                console.error('Ошибка при проверке статуса бронирования:', errorData);
                console.log({
                    title: 'Ошибка',
                    text: 'Не удалось проверить статус бронирования.',
                    icon: 'error',
                    confirmButtonText: 'Ок'
                });
                alert(`не удалось проверить статус: ${errorData}`);
                clearInterval(intervalId);
            }
        } catch (error) {
            console.error('Ошибка при проверке статуса бронирования:', error);
            console.log({
                title: 'Ошибка',
                text: 'Произошла ошибка при проверке статуса бронирования.',
                icon: 'error',
                confirmButtonText: 'Ок'
            });
            alert(`не удалось проверить статус: ${error.message}`);
            clearInterval(intervalId);
        }

        attempts++;
        if (attempts >= maxAttempts) {
            console.error('Время ожидания ответа от сервера истекло.');
            console.log({
                title: 'Время истекло',
                text: 'Время ожидания ответа от сервера истекло.',
                icon: 'warning',
                confirmButtonText: 'Ок'
            });
            clearInterval(intervalId);
        }
    }, pollingInterval);
}

/**
 * Функция для отображения сообщения об успешном действии
 * @param {string} message - Текст сообщения
 */
function showSuccessMessage(message) {
    let successMessage = document.getElementById('success-message');
    if (!successMessage) {
        successMessage = document.createElement('success-message')
    }
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
 * Функция для отображения сообщения об ошибке
 * @param {string} message - Текст сообщения
 */
function showErrorMessage(message) {
    const errorMessage = document.getElementById('error-message');
    errorMessage.textContent = message;
    errorMessage.classList.remove('success');
    errorMessage.classList.add('error');
    errorMessage.style.display = 'block';

    // Скрыть сообщение через 5 секунд
    setTimeout(() => {
        errorMessage.style.display = 'none';
    }, 5000);
}


function formatTimeSlot(slot) {
    const start = formatTime(slot.startTime);
    const end = formatTime(slot.endTime);
    return `${start} - ${end}`;
}

function formatTime(timeStr) {
    const [hours, minutes] = timeStr.split(':');
    return `${hours}:${minutes}`;
}

function getDayOfWeekName(dayOfWeek) {
    const days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
    return days[dayOfWeek - 1] || 'Неизвестный день';
}

function groupBy(array, key) {
    return array.reduce((result, currentItem) => {
        const groupKey = currentItem[key];
        if (!result[groupKey]) {
            result[groupKey] = [];
        }
        result[groupKey].push(currentItem);
        return result;
    }, {});
}