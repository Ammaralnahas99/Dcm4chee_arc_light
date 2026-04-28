import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { J4careHttpService } from '../helpers/j4care-http.service';
import { AppService } from '../app.service';

interface Hospital {
    name: string;
    location?: string;
    patients: number;
    studies: number;
    modalities?: string[];
    departments?: string[];
    lastStudyDate?: string;
    active: boolean;
}

@Component({
    selector: 'app-hospital-dashboard',
    templateUrl: './hospital-dashboard.component.html',
    styleUrls: ['./hospital-dashboard.component.scss'],
    standalone: true,
    imports: [CommonModule]
})
export class HospitalDashboardComponent implements OnInit {
    hospitals: Hospital[] = [];
    loading: boolean = true;
    error: string = '';
    specificHospital: string | null = null;

    constructor(
        private $http: J4careHttpService,
        private mainservice: AppService,
        private route: ActivatedRoute
    ) { }

    ngOnInit(): void {
        // Check if we're viewing a specific hospital
        this.route.params.subscribe(params => {
            this.specificHospital = params['hospitalName'] || null;
            this.loadHospitalStatistics();
        });
    }

    loadHospitalStatistics(): void {
        this.loading = true;
        this.error = '';

        let url: string;
        const aetTitle = this.mainservice.archiveDeviceName || 'dcm4chee-arc';

        if (this.specificHospital) {
            // Load specific hospital statistics
            url = `/dcm4chee-arc/aets/${aetTitle}/rs/hospitals/${encodeURIComponent(this.specificHospital)}/statistics`;

            this.$http.get(url).subscribe({
                next: (data: any) => {
                    this.hospitals = [{
                        name: data.name,
                        patients: data.patients || 0,
                        studies: data.studies || 0,
                        active: data.active !== false,
                        modalities: [],
                        departments: []
                    }];

                    // Load modalities for the hospital
                    this.loadHospitalModalities(this.hospitals[0]);

                    this.loading = false;
                },
                error: (err) => {
                    console.error('Error loading hospital statistics:', err);
                    this.error = 'Failed to load hospital statistics. Please try again.';
                    this.loading = false;
                }
            });
        } else {
            // Load all hospitals statistics
            url = `/dcm4chee-arc/aets/${aetTitle}/rs/hospitals/statistics`;

            this.$http.get(url).subscribe({
                next: (data: any[]) => {
                    this.hospitals = data.map(hospital => ({
                        name: hospital.name,
                        patients: hospital.patients || 0,
                        studies: hospital.studies || 0,
                        active: hospital.active !== false,
                        modalities: [],
                        departments: []
                    }));

                    // Load modalities for each hospital
                    this.hospitals.forEach(hospital => {
                        this.loadHospitalModalities(hospital);
                    });

                    this.loading = false;
                },
                error: (err) => {
                    console.error('Error loading hospital statistics:', err);
                    this.error = 'Failed to load hospital statistics. Please try again.';
                    this.loading = false;
                }
            });
        }
    }

    loadHospitalModalities(hospital: Hospital): void {
        const aetTitle = this.mainservice.archiveDeviceName || 'dcm4chee-arc';
        const url = `/dcm4chee-arc/aets/${aetTitle}/rs/hospitals/${encodeURIComponent(hospital.name)}/modalities`;

        this.$http.get(url).subscribe({
            next: (modalities: string[]) => {
                hospital.modalities = modalities;
            },
            error: (err) => {
                console.error(`Error loading modalities for ${hospital.name}:`, err);
            }
        });
    }

    viewHospitalDetails(hospital: Hospital): void {
        console.log('View details for:', hospital.name);
        // Navigate to studies filtered by hospital
        window.location.href = `#/study/study?hospitalName=${encodeURIComponent(hospital.name)}`;
    }
}
