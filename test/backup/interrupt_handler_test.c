/* Тестовый файл для проверки обработчиков прерываний */

// Объявление обработчика прерывания для таймера 0
void timer0_isr() interrupt 1 {
    static int counter = 0;
    counter++;
    
    // Инвертируем бит на порту P1
    P1 ^= 0x01;
}

// Объявление обработчика прерывания для UART
void uart_isr() interrupt 2 {
    // Проверяем статус прерывания
    if (SCON & 0x01) {  // Прерывание по приему
        char data = SBUF;  // Считываем данные
        
        // Обработка полученных данных
        P2 = data;  // Вывод данных на порт P2
    }
    
    // Сброс флага прерывания
    SCON &= ~0x01;
}

// Функция инициализации прерываний
void init_interrupts() {
    // Разрешаем глобальные прерывания
    EA = 1;
    
    // Разрешаем прерывания от таймера 0
    ET0 = 1;
    
    // Разрешаем прерывания от UART
    ES = 1;
    
    // Настраиваем таймер 0
    TMOD = 0x01;  // Режим 1 (16-битный таймер)
    TH0 = 0xFC;   // Загружаем начальное значение для 1 мс
    TL0 = 0x66;
    TR0 = 1;      // Запускаем таймер
}

// Главная функция
int main() {
    // Инициализация прерываний
    init_interrupts();
    
    // Бесконечный цикл
    while (1) {
        // Основной код программы
        // ...
    }
    
    return 0;
} 