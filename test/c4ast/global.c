#include <8051.h>

char global_var = 10;

void main() {
    P1 = global_var;
    global_var = 20;
    P2 = global_var;

    while(1);
}
