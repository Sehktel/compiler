#include <reg2051.h> /*  #define IE0_VECTOR 0  */

/* Объявление специальных регистров */
sfr SP = 0x81;    // Указатель стека
sfr P1 = 0x90;    // Порт P1

/* Объявление битов регистров */
sbit P1_0 = 0x90; // Бит 0 порта P1
sbit EA = 0xAF;   // Global Interrupt Enable
sbit EX0 = 0xA8;  // External Interrupt 0 Enable
sbit IT0 = 0x88;  // External Interrupt 0 Type (1=edge, 0=level)

/* Обработчик прерывания INT0 (P3.2) */
void P14 (void) interrupt IE0_VECTOR {
	while(1) {
		P1 = 0x04;
	}
}

/* Основная функция */
void main(void) {
	EA = 1;    // Разрешаем глобальные прерывания
	EX0 = 1;   // Разрешаем внешнее прерывание 0
	IT0 = 1;   // Прерывание по фронту
	while(1) {
		P1 = 0x08;
	}
} 