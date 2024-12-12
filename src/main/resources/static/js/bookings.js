let selectedMachineId = null;

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


async function handleBooking(event) {
    const machineId = event.target.getAttribute('data-machine-id');
    const resDate = event.target.getAttribute('data-res-date');
    const startTime = event.target.getAttribute('data-start-time');
    const endTime = event.target.getAttribute('data-end-time');

    if (!machineId || !resDate || !startTime || !endTime) {
        console.error('Недостаточно данных для бронирования.');
        return;
    }

    if (!confirm('Вы уверены, что хотите забронировать этот слот?')) {
        return;
    }

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

        if (response.ok) {
            await loadMachineTimeSlots();
        } else {
            const errorData = await response.json();
            alert(`Ошибка при бронировании: ${errorData.message || 'Неизвестная ошибка.'}`);
        }
    } catch (error) {
        console.error('Ошибка при бронировании:', error);
        alert('Произошла ошибка при бронировании. Пожалуйста, попробуйте позже.');
    }
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