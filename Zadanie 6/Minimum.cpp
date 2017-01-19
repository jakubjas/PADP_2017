#include"Minimum.h"
#include<stdlib.h>   // potrzebne dla random()
#include<math.h>     // bo sin i cos
#include<iostream>
#include <sys/time.h>

Minimum::Minimum( Function *f, double min, double max ) : min( min ), max( max ), f(f) {
   bestX = bestY = ( min + max ) * 0.5;
   bestV = f->value( bestX, bestY );
   counter = 0;
   sumV = 0;
   sumX = 0;
   sumY = 0;
}

// metoda zwraca pozycje z wnetrza obszaru poszukiwania
double Minimum::limit( double x ) {
   if ( x < min ) return min; // za male
   if ( x > max ) return max; // za duze
   return x; // x jest pomiedzy min a max, wiec zwracamy x
}

bool Minimum::haveTimeToContinue() {
  struct timeval tf;
  gettimeofday( &tf, NULL );
  double now = tf.tv_sec * 1000 + tf.tv_usec * 0.001;

  if ( now < timeLimit ) return true;  // limit czasu nie osiagniety
  return false; // juz po czasie, pora konczyc obliczenia
}

void Minimum::initializeTimeLimit( double msec ) {
  struct timeval tf;
  gettimeofday( &tf, NULL );
  timeLimit = tf.tv_sec * 1000 + tf.tv_usec * 0.001 + msec; // ustawiamy czas zakonczenia obliczen
}

void Minimum::find( double dr, int idleStepsLimit, double msec ) {

  long* tempCounter;
  double* tempSumV;
  double* tempSumX;
  double* tempSumY;

  // ustalamy czas zakonczenia obliczen na msec od teraz
  initializeTimeLimit( msec );

  #pragma omp parallel
  {
    double x, y, v, r, xnew, ynew, vnew;
    struct drand48_data buff;
    struct timeval tv;
    int s = omp_get_num_threads();
    int n = omp_get_thread_num();

    #pragma omp single
    {
      tempCounter = new long[ s ];
      tempSumV = new double[ s ];
      tempSumX = new double[ s ];
      tempSumY = new double[ s ];

      for ( int i = 0; i < s; i++ )
      {
        tempCounter[ i ] = 0;
        tempSumV[ i ] = 0.0;
        tempSumX[ i ] = 0.0;
        tempSumY[ i ] = 0.0;
      }
    }

    gettimeofday(&tv, NULL);
    srand48_r(tv.tv_sec * n + tv.tv_usec, &buff);

    while ( haveTimeToContinue() ) {
      // inicjujemy losowo polozenie startowe w obrebie kwadratu o bokach od min do max
      drand48_r(&buff, &x);
      drand48_r(&buff, &y);
      x = x * ( max - min ) + min;
      y = y * ( max - min ) + min;

      v = f->value( x, y ); // wartosc funkcji w punkcie startowym

      int idleSteps = 0;  // liczba krokow, ktore nie poprawily lokalizacji

      while ( idleSteps < idleStepsLimit ) {
         drand48_r(&buff, &r);
         r *= 6.28318;  // liczba losowa do 0 do 2 pi

         // oto nowe polozenie w odleglosci dr od poprzedniego
         xnew = x + dr * sin( r );
         ynew = y + dr * cos( r );

         // upewniamy sie, ze nie opuscilismy przestrzeni poszukiwania rozwiazania
         xnew = limit( xnew );
         ynew = limit( ynew );

         // wartosc funkcji w nowym polozeniu
         vnew = f->value( xnew, ynew );

         // informacje o badanych położeniach i uzyskiwanych wartościach
         tempCounter[n]++;
         tempSumV[n] += vnew;
         tempSumX[n] += xnew;
         tempSumY[n] += ynew;

         if ( vnew < v ) {
           x = xnew;  // przenosimy sie do nowej, lepszej lokalizacji
           y = ynew;
           v = vnew;
           idleSteps = 0; // resetujemy licznik krokow, bez poprawy polozenia
         } else {
           idleSteps++; // nic sie nie stalo
         }
      } // przez idleStepsLimit nic znalezlismy lepszej pozycji

     #pragma omp critical
     {
       if ( v < bestV ) {  // znalezlismy najlepsze polozenie globalnie
          bestV = v;
          bestX = x;
          bestY = y;

          std::cout << "Thread #" << omp_get_thread_num() << " new better position: " << x << ", " << y << " value = " << v << std::endl;
       }
     }
    }

    #pragma omp barrier
    #pragma omp single
    {
      for ( int th = 0; th < s; th++ )
      {
        counter += tempCounter[ th ];
        sumV += tempSumV[ th ];
        sumX += tempSumX[ th ];
        sumY += tempSumY[ th ];
      }

      delete[] tempCounter;
      delete[] tempSumV;
      delete[] tempSumX;
      delete[] tempSumY;
    }
  } //parallel
}
