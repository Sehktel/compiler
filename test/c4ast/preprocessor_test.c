/* Тестовый файл для проверки директив препроцессора и заголовочных файлов */

// Подключение стандартных заголовочных файлов
#include <stdio.h>
#include <stdlib.h>

// Определение макросов
#define PI 3.14159
#define SQUARE(x) ((x) * (x))
#define MAX(a, b) ((a) > (b) ? (a) : (b))

// Условная компиляция
#ifdef DEBUG
    #define LOG(msg) printf("DEBUG: %s\n", msg)
#else
    #define LOG(msg)
#endif

#ifndef VERSION
    #define VERSION "1.0.0"
#endif

// Вложенные условные директивы
#if defined(WINDOWS)
    #define PLATFORM "Windows"
#elif defined(LINUX)
    #define PLATFORM "Linux"
#else
    #define PLATFORM "Unknown"
#endif

int main() {
    // Использование определенных макросов
    double radius = 5.0;
    double area = PI * SQUARE(radius);
    
    int a = 10, b = 20;
    int max_val = MAX(a, b);
    
    // Использование условной компиляции
    LOG("Program started");
    
    printf("Platform: %s\n", PLATFORM);
    printf("Version: %s\n", VERSION);
    printf("Area: %f\n", area);
    printf("Max value: %d\n", max_val);
    
    return 0;
} 