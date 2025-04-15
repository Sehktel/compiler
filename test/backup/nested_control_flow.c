/* Тестовый файл для проверки вложенных структур управления */

int main() {
    int a = 5;
    int b = 10;
    int result = 0;
    
    // Вложенные условные операторы
    if (a < b) {
        if (a > 0) {
            result = 1;
        } else {
            result = 2;
        }
    } else {
        if (b > 0) {
            result = 3;
        } else {
            result = 4;
        }
    }
    
    // Вложенные циклы
    int i = 0;
    int j = 0;
    
    while (i < 3) {
        j = 0;
        while (j < 3) {
            result = result + i * j;
            j = j + 1;
        }
        i = i + 1;
    }
    
    // Условие внутри цикла
    i = 0;
    while (i < 5) {
        if (i % 2 == 0) {
            result = result + 1;
        } else {
            result = result - 1;
        }
        i = i + 1;
    }
    
    return result;
} 