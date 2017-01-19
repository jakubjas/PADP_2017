/*
 * Optimize.cpp
 *
 *      Author: Jakub Jas
 */

#include "Optimize.h"
#include "Consts.h"
#include <math.h>
#include <mpi.h>
#include <iostream>

using namespace std;

Optimize::Optimize() {
    x = new double[POSITIONS];
    y = new double[POSITIONS];
    z = new double[POSITIONS];
    fieldV = new double[POSITIONS];
	xp = new double[PARTICLES];
	yp = new double[PARTICLES];
	zp = new double[PARTICLES];
	xpp = new double[PARTICLES];
	ypp = new double[PARTICLES];
	zpp = new double[PARTICLES];
}

void Optimize::setPositionsAndRequiredFieldValues(const double *x0, const double *y0,
		const double *z0, const double *v0) {
	this->x = x0;
	this->y = y0;
	this->z = z0;
	this->fieldV = v0;
}

void Optimize::setMC(MonteCarlo * mc) {
	this->mc = mc;
}

double *Optimize::getX() {
	return xp;
}
double *Optimize::getY() {
	return yp;
}
double *Optimize::getZ() {
	return zp;
}

void Optimize::setParticlesInitialPositions(const double *x0, const double *y0,
		const double *z0) {
	for (int i = 0; i < PARTICLES; i++) {
		xp[i] = x0[i];
		yp[i] = y0[i];
		zp[i] = z0[i];
	}
}

void Optimize::calcInitialError() {
	for (int i = 0; i < PARTICLES; i++) {
		xpp[i] = xp[i];
		ypp[i] = yp[i];
		zpp[i] = zp[i];
	}
    
    double dv, delta = 0;
    for (int i = 0; i < POSITIONS; i++) {
        dv = fieldV[i] - Helper::value(xpp, ypp, zpp, x[i], y[i], z[i]);
        delta += dv * dv;
    }
    
	err = sqrt(delta) / POSITIONS;
}

void Optimize::updateParticlesPositions() {
	for (int i = 0; i < PARTICLES; i++) {
		xp[i] = xpp[i];
		yp[i] = ypp[i];
		zp[i] = zpp[i];
	}
}

void Optimize::shareFieldData() {
    MPI_Bcast((double*)x,POSITIONS,MPI_DOUBLE,0,MPI_COMM_WORLD);
    MPI_Bcast((double*)y,POSITIONS,MPI_DOUBLE,0,MPI_COMM_WORLD);
    MPI_Bcast((double*)z,POSITIONS,MPI_DOUBLE,0,MPI_COMM_WORLD);
    MPI_Bcast((double*)fieldV,POSITIONS,MPI_DOUBLE,0,MPI_COMM_WORLD);
}


double Optimize::calcError() {
    
    int rank;
    int processes;
    
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &processes);
    
    int *part_size = new int[processes];
    
    // petla okreslajaca podzial tablicy na mniejsze czesci
    for (int i=0; i<processes; i++)
    {
        part_size[i] = POSITIONS / processes;
        
        if (i < POSITIONS % processes)
            part_size[i] += 1;
    }
    
    int begin = 0;
    int end = 0;
    
    for (int i=0; i<=rank; i++)
        end += part_size[i];
    
    begin = end - part_size[rank];
    end -= 1; // konieczne z uwagi na numerowanie tablicy od 0
    
    double dv, local_delta, delta = 0;
    for (int i = begin; i <= end; i++) {
        dv = fieldV[i] - Helper::value(xpp, ypp, zpp, x[i], y[i], z[i]);
        local_delta += dv * dv;
    }
    
    // zebranie wynikow czesciowych i ich zsumowanie
    MPI_Reduce(&local_delta,&delta,1,MPI_DOUBLE,MPI_SUM,0,MPI_COMM_WORLD);
    
    if ( rank == 0 )
    {
        return sqrt(delta)/POSITIONS;
    }
    
    return 0;
}

void Optimize::step() {
    
    int rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    
    // dostep do obiektu klasy MonteCarlo tylko dla procesu rank = 0
    if ( rank == 0 )
        mc->generateNewPositions(xp, yp, zp, xpp, ypp, zpp);
    
    MPI_Bcast(xpp,PARTICLES,MPI_DOUBLE,0,MPI_COMM_WORLD);
    MPI_Bcast(ypp,PARTICLES,MPI_DOUBLE,0,MPI_COMM_WORLD);
    MPI_Bcast(zpp,PARTICLES,MPI_DOUBLE,0,MPI_COMM_WORLD);
    
    double newErr = calcError();
    
    // dostep do obiektu klasy MonteCarlo tylko dla procesu rank = 0
    if ( rank == 0 )
    {
        if (mc->accept(err, newErr)) {
            err = newErr;
            updateParticlesPositions();
        }
    }
}

Optimize::~Optimize() {
    delete[] x;
    delete[] y;
    delete[] z;
    delete[] fieldV;
    delete[] xp;
    delete[] yp;
    delete[] zp;
    delete[] xpp;
    delete[] ypp;
    delete[] zpp;
}
