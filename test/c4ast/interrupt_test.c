#include <reg2051.h>

/* Определения векторов прерываний */
#define IE0_VECTOR	0  /* Внешнее прерывание 0 */
#define TF0_VECTOR	1  /* Прерывание таймера 0 */

/* Объявление специальных регистров */
sfr P1 = 0x90;    // Порт P1
sfr SP = 0x81;    // Указатель стека
sfr IE = 0xA8;    // Регистр разрешения прерываний
sfr TCON = 0x88;  // Регистр управления таймером

/* Объявление битов регистров */
sbit EA = 0xAF;   // Global Interrupt Enable (IE.7)
sbit EX0 = 0xA8;  // External Interrupt 0 Enable (IE.0)
sbit ET0 = 0xA9;  // Timer 0 Interrupt Enable (IE.1)
sbit IT0 = 0x88;  // External Interrupt 0 Type (TCON.0, 1=edge, 0=level)
sbit TR0 = 0x8C;  // Timer 0 Run (TCON.4)
sbit TF0 = 0x8D;  // Timer 0 Flag (TCON.5)

/* Обработчик внешнего прерывания INT0 (P3.2) */
void ext_int0() interrupt IE0_VECTOR {
    P1 = ~P1;  // Инвертируем состояние порта P1
}

/* Обработчик прерывания таймера 0 */
void timer0_isr() interrupt TF0_VECTOR {
    static unsigned char counter = 0;
    
    counter++;
    if (counter >= 10) {
        counter = 0;
        P1 = ~P1;  // Инвертируем состояние порта P1 каждые 10 прерываний
    }
}

/* Основная функция */
void main(void) {
    // Инициализация
    P1 = 0x00;     // Сбрасываем порт P1
    
    // Настройка прерываний
    EA = 1;        // Разрешаем глобальные прерывания
    EX0 = 1;       // Разрешаем внешнее прерывание 0
    ET0 = 1;       // Разрешаем прерывание таймера 0
    IT0 = 1;       // Прерывание INT0 по фронту
    
    // Настройка таймера 0
    TR0 = 1;       // Запускаем таймер 0
    
    // Основной цикл
    while(1) {
        // Ничего не делаем, ждем прерываний
    }
} 