#include "Consts.h"
#include <iostream>
#include <stdio.h>
#include <time.h>
#include <stdlib.h>
#include <sys/time.h>
#include <mpi.h>

#include "MonteCarlo.h"
#include "Generator.h"
#include "Optimize.h"

using namespace std;

long MonteCarlo::steps = 0;
double MonteCarlo::scaleFactor = 4.0;
Generator *gen;
Optimize *op;
MonteCarlo *mc;


int main(int argc, char **argv) {

	MPI_Init(&argc, &argv);

	int rank;
	int processes;

	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &processes);

	struct timeval tf;
	double t0, serialExecTime, parallelExecTime;

	op = new Optimize();

	if ( ! rank ) {
		srandom(time( NULL));
		gen = new Generator();
		mc = new MonteCarlo();

		gettimeofday(&tf, NULL);
		t0 = tf.tv_sec + tf.tv_usec * 0.000001;
		double errorIni = gen->calcInitialFieldError();
		gettimeofday(&tf, NULL);
		serialExecTime = tf.tv_sec + tf.tv_usec * 0.000001 - t0;

		double errorPosIni = gen->error( gen->getXop(), gen->getYop(), gen->getZop() );
		mc->setScaleFactor( errorIni );

		op->setMC( mc );

		op->setPositionsAndRequiredFieldValues( gen->getX(), gen->getY(), gen->getZ(), gen->getV() );
		op->setParticlesInitialPositions( gen->getXop(), gen->getYop(), gen->getZop() );
		op->calcInitialError();

		op->shareFieldData();

		gettimeofday(&tf, NULL);
		t0 = tf.tv_sec + tf.tv_usec * 0.000001;
		for ( int i = 0; i < NUMBER_OF_REPETITIONS; i++ ) {
			op->step();
			if ( ! ( i % HEARTBEAT ) ) cout << "Steps done : " << i << endl;
		}
		gettimeofday(&tf, NULL);
		parallelExecTime = ( tf.tv_sec + tf.tv_usec * 0.000001 - t0 ) / NUMBER_OF_REPETITIONS;
		cout << "Initial Field     error : " << errorIni << endl;
		cout << "Final   Field     error : " << gen->calcFinalFieldError( op->getX(), op->getY(), op->getZ() ) << endl;
		cout << "Initial Positions error : " << errorPosIni << endl;
		cout << "Final   Positions error : " << gen->error( op->getX(), op->getY(), op->getZ() ) << endl;
		cout << "Serial  execution time  : " << serialExecTime << endl;
		cout << "Serial steps per 1sec   : " << 1.0/serialExecTime << endl;
		cout << "Parallel execution time : " << parallelExecTime << endl;
		cout << "Parallel steps per 1 sec: " << 1.0/parallelExecTime << endl;
		cout << "Speedup                 : " << serialExecTime / parallelExecTime << endl;
		cout << "Efficiency              : " << ( serialExecTime / ( parallelExecTime * processes )) << endl;
	} else {
		op->shareFieldData();
		for ( int i = 0; i < NUMBER_OF_REPETITIONS; i++ ) {
			op->step();
		}
	}

   MPI_Finalize();

}
