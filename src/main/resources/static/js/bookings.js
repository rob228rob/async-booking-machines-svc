let selectedMachineId = null;
let pollingInterval = 150; // Интервал в миллисекундах
let maxAttempts = 5; // Максимальное количество попыток

document.addEventListener('DOMContentLoaded', () => {
    loadMachines().then(() => console.log("machines loaded"));
});

/**
 * Загрузка списка «машинок» (пусть остаётся /api/machines/get-all)
 */
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

/**
 * Отображение списка машинок
 * Предполагается, что бэкенд возвращает JSON с полями:
 * - id
 * - name
 * - locationName (бывшее dormitoryName)
 * - machineType
 */
function displayMachinesList(machines) {
    const machinesList = document.getElementById('machines-list');
    machinesList.innerHTML = '';

    machines.forEach(machine => {
        const machineCard = document.createElement('div');
        machineCard.classList.add('machine-card');
        machineCard.innerHTML = `
            <h3>${machine.name}</h3>
            <p><strong>Локация:</strong> ${machine.locationName || 'Неизвестно'}</p>
            <p><strong>Тип машинки:</strong> ${machine.machineType || 'Неизвестно'}</p>
        `;

        // При клике по карточке выбранная машинка запоминается, и показываем блок выбора слотов
        machineCard.addEventListener('click', () => {
            selectedMachineId = machine.id;
            document.getElementById('slots-controls').style.display = 'flex';
            document.getElementById('machines-container').innerHTML = '';
        });

        machinesList.appendChild(machineCard);
    });

    // Привязываем обработчик кнопки "Загрузить слоты"
    const loadSlotsButton = document.getElementById('load-slots-button');
    loadSlotsButton.addEventListener('click', loadMachineTimeSlots);
}

/**
 * Загрузка временных слотов конкретной машинки
 */
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

    // Предположим, что слоты тоже идут через /api/machine-slots/get
    const url = `/api/machine-slots/get?machineId=${selectedMachineId}&startDate=${startDate}&weeks=${weeks}`;
    try {
        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include',
        });
        if (response.ok) {
            const data = await response.json();
            // Предположим, бэкенд возвращает объект { slots: [...] }
            displayGeneratedSlots(data.slots);
        } else {
            console.error('Не удалось получить слоты для машинки.');
        }
    } catch (error) {
        console.error('Ошибка при получении слотов для машинки:', error);
    }
}

/**
 * Отображение слотов
 * Здесь, исходя из структуры данных, меняем "dormitoryName" → "locationName" и т.д.
 */
function displayGeneratedSlots(machines) {
    const machinesContainer = document.getElementById('machines-container');
    machinesContainer.innerHTML = '';

    const { startDate, weeks } = loadMachineTimeSlots;

    // Генерируем все даты для выбранного периода
    const allDates = [];
    const start = new Date(startDate);
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7)); // понедельник

    for (let week = 0; week < weeks; week++) {
        for (let day = 1; day <= 7; day++) {
            const date = new Date(start);
            date.setDate(start.getDate() + week * 7 + (day - 1));
            allDates.push({
                dayOfWeek: day,
                date: date.toISOString().split('T')[0]
            });
        }
    }

    const dateMap = {};
    allDates.forEach((item, index) => {
        const weekNumber = Math.floor(index / 7) + 1;
        dateMap[`${item.dayOfWeek}-${weekNumber}`] = item.date;
    });

    machines.forEach((machine) => {
        machine.timeSlots.forEach((slot, index) => {
            const weekNumber = Math.floor((index / machine.timeSlots.length) * weeks) + 1;
            const resDate = dateMap[`${slot.dayOfWeek}-${weekNumber}`] || '';

            if (!resDate) {
                console.error(`Не удалось определить дату для dayOfWeek: ${slot.dayOfWeek}, weekNumber: ${weekNumber}`);
                return;
            }

            // Проверяем, создан ли уже блок для этой "машинки"
            let machineCard = machinesContainer.querySelector(`[data-machine-id="${machine.machineId}"]`);
            if (!machineCard) {
                machineCard = document.createElement('div');
                machineCard.classList.add('machine-card');
                machineCard.setAttribute('data-machine-id', machine.machineId);
                machineCard.innerHTML = `
                    <h3>${machine.machineName}</h3>
                    <p><strong>Локация:</strong> ${machine.locationName}</p>
                    <ul class="time-slots"></ul>
                `;
                machinesContainer.appendChild(machineCard);
            }

            const timeSlotsList = machineCard.querySelector('.time-slots');
            const timeSlotItem = document.createElement('li');
            timeSlotItem.classList.add(
                'time-slot',
                slot.available ? 'available' : 'booked'
            );
            timeSlotItem.innerHTML = `
                <strong>${getDayOfWeekName(slot.dayOfWeek)} (${resDate})</strong>:
                <span>${slot.startTime} - ${slot.endTime}</span>
                ${
                slot.available
                    ? `<button class="book"
                              data-machine-id="${machine.machineId}"
                              data-res-date="${resDate}"
                              data-start-time="${slot.startTime}"
                              data-end-time="${slot.endTime}">
                          Забронировать
                       </button>`
                    : `<button class="booked" disabled>Забронировано</button>`
            }
            `;
            timeSlotsList.appendChild(timeSlotItem);
        });
    });

    // Назначаем обработчики на все кнопки «Забронировать»
    document.querySelectorAll('.book').forEach(button => {
        button.addEventListener('click', handleBooking);
    });
}

/**
 * Обработка бронирования слота
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

    const reservationRequest = {
        // Если бекэнд ждёт machineId – так и оставляем
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

        if (response.status === 202) {
            // ...
            const statusUrl = response.headers.get('Location');
            const reservationId = extractReservationIdFromUrl(statusUrl);
            showSuccessMessage('Бронирование инициировано. Ожидайте обновления статуса.');

            pollReservationStatus(reservationId, statusUrl);
        } else if (response.status === 400) {
            alert('Невалидная дата (например, в прошлом).');
        } else {
            const errorData = await response.json();
            alert(`Ошибка при бронировании: ${errorData.message || 'Неизвестная ошибка.'}`);
        }
    } catch (error) {
        console.error('Ошибка при бронировании:', error);
        alert('Произошла ошибка при бронировании.');
    }
}

/**
 * Пример поллинга статуса
 */
function extractReservationIdFromUrl(statusUrl) {
    const parts = statusUrl.split('/');
    return parts[parts.length - 1];
}

async function pollReservationStatus(reservationId, statusUrl) {
    let attempts = 0;
    const intervalId = setInterval(async () => {
        try {
            const response = await fetch(statusUrl, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                // Если 2xx, получаем текст
                const statusText = await response.text();
                if (statusText === 'Operation completed successfully') {
                    clearInterval(intervalId);
                    // Обновить слоты
                    await loadMachineTimeSlots();
                } else if (statusText.startsWith('Operation failed')) {
                    clearInterval(intervalId);
                    alert(`Бронирование не удалось: ${statusText}`);
                } else {
                    console.log(`Текущее состояние [${reservationId}]: ${statusText}`);
                }
            } else {
                // Ошибка: читаем текст, потому что сервер может вернуть text/plain
                const errorText = await response.text();
                alert(`Ошибка при проверке статуса: ${errorText}`);
                clearInterval(intervalId);
            }

        } catch (error) {
            console.error('Ошибка при поллинге:', error);
            clearInterval(intervalId);
        }

        attempts++;
        if (attempts >= maxAttempts) {
            console.warn('Время ожидания ответа от сервера истекло.');
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