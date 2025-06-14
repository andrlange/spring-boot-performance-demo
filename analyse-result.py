#!/usr/bin/env python3
"""
Performance Analysis Script for Thread Comparison Tests
Analyzes Apache Bench results and generates comparison reports
"""

import os
import re
import json
import csv
import sys
from datetime import datetime
from pathlib import Path

class PerformanceAnalyzer:
    def __init__(self, results_dir="performance_results"):
        self.results_dir = Path(results_dir)
        self.tests = {}

    def parse_ab_results(self, file_path):
        """Parse Apache Bench results file"""
        with open(file_path, 'r') as f:
            content = f.read()

        # Extract key metrics using regex
        metrics = {}

        # Requests per second
        rps_match = re.search(r'Requests per second:\s+([0-9.]+)\s+\[#/sec\]', content)
        metrics['requests_per_second'] = float(rps_match.group(1)) if rps_match else 0

        # Time per request (mean)
        tpr_mean_match = re.search(r'Time per request:\s+([0-9.]+)\s+\[ms\]\s+\(mean\)', content)
        metrics['time_per_request_mean'] = float(tpr_mean_match.group(1)) if tpr_mean_match else 0

        # Time per request (mean, across all concurrent requests)
        tpr_concurrent_match = re.search(r'Time per request:\s+([0-9.]+)\s+\[ms\]\s+\(mean, across all concurrent requests\)', content)
        metrics['time_per_request_concurrent'] = float(tpr_concurrent_match.group(1)) if tpr_concurrent_match else 0

        # Failed requests
        failed_match = re.search(r'Failed requests:\s+([0-9]+)', content)
        metrics['failed_requests'] = int(failed_match.group(1)) if failed_match else 0

        # Total requests
        total_match = re.search(r'Complete requests:\s+([0-9]+)', content)
        metrics['total_requests'] = int(total_match.group(1)) if total_match else 0

        # Concurrency level
        concurrency_match = re.search(r'Concurrency Level:\s+([0-9]+)', content)
        metrics['concurrency_level'] = int(concurrency_match.group(1)) if concurrency_match else 0

        # Percentiles
        percentile_section = re.search(r'Percentage of the requests served within a certain time \(ms\)\s+(.*?)(?=\n\n|\Z)', content, re.DOTALL)
        if percentile_section:
            percentile_text = percentile_section.group(1)
            percentiles = {}
            for line in percentile_text.split('\n'):
                if '%' in line:
                    parts = line.strip().split()
                    if len(parts) >= 2:
                        try:
                            pct = parts[0].replace('%', '')
                            time_ms = parts[1]
                            percentiles[f'p{pct}'] = float(time_ms)
                        except (ValueError, IndexError):
                            continue
            metrics['percentiles'] = percentiles

        return metrics

    def load_all_results(self):
        """Load all test results from the results directory"""
        if not self.results_dir.exists():
            print(f"Results directory {self.results_dir} not found!")
            return

        for result_file in self.results_dir.glob("*_results.txt"):
            test_name = result_file.stem.replace('_results', '')
            try:
                metrics = self.parse_ab_results(result_file)
                self.tests[test_name] = metrics
                print(f"Loaded results for: {test_name}")
            except Exception as e:
                print(f"Error parsing {result_file}: {e}")

    def generate_comparison_report(self):
        """Generate a detailed comparison report"""
        if not self.tests:
            print("No test results loaded!")
            return

        # Sort tests by RPS (descending)
        sorted_tests = sorted(self.tests.items(), key=lambda x: x[1]['requests_per_second'], reverse=True)

        report = []
        report.append("=" * 80)
        report.append("THREAD PERFORMANCE COMPARISON REPORT")
        report.append("=" * 80)
        report.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")

        # Summary table
        report.append("PERFORMANCE SUMMARY (sorted by Requests/sec)")
        report.append("-" * 80)
        report.append(f"{'Test Name':<25} {'RPS':<10} {'Mean(ms)':<10} {'95th%':<10} {'Failed':<8}")
        report.append("-" * 80)

        for test_name, metrics in sorted_tests:
            rps = metrics['requests_per_second']
            mean_time = metrics['time_per_request_mean']
            p95 = metrics.get('percentiles', {}).get('p95', 'N/A')
            failed = metrics['failed_requests']

            report.append(f"{test_name:<25} {rps:<10.1f} {mean_time:<10.1f} {p95:<10} {failed:<8}")

        report.append("")

        # Detailed analysis
        report.append("DETAILED ANALYSIS")
        report.append("-" * 50)

        best_rps = sorted_tests[0]
        worst_rps = sorted_tests[-1]

        report.append(f"Best throughput: {best_rps[0]} ({best_rps[1]['requests_per_second']:.1f} RPS)")
        report.append(f"Worst throughput: {worst_rps[0]} ({worst_rps[1]['requests_per_second']:.1f} RPS)")

        improvement = (best_rps[1]['requests_per_second'] / worst_rps[1]['requests_per_second'] - 1) * 100
        report.append(f"Performance improvement: {improvement:.1f}%")
        report.append("")

        # Threading model analysis
        java_tests = {k: v for k, v in self.tests.items() if 'java' in k}
        kotlin_tests = {k: v for k, v in self.tests.items() if 'kotlin' in k}

        if java_tests:
            report.append("JAVA IMPLEMENTATIONS:")
            for test_name, metrics in java_tests.items():
                report.append(f"  {test_name}: {metrics['requests_per_second']:.1f} RPS")

        if kotlin_tests:
            report.append("")
            report.append("KOTLIN IMPLEMENTATIONS:")
            for test_name, metrics in kotlin_tests.items():
                report.append(f"  {test_name}: {metrics['requests_per_second']:.1f} RPS")

        # Threading model comparison
        report.append("")
        report.append("THREADING MODEL INSIGHTS:")

        virtual_tests = {k: v for k, v in self.tests.items() if 'virtual' in k}
        platform_tests = {k: v for k, v in self.tests.items() if 'platform' in k}
        coroutine_tests = {k: v for k, v in self.tests.items() if 'coroutine' in k}

        if virtual_tests and platform_tests:
            avg_virtual = sum(t['requests_per_second'] for t in virtual_tests.values()) / len(virtual_tests)
            avg_platform = sum(t['requests_per_second'] for t in platform_tests.values()) / len(platform_tests)
            virtual_improvement = (avg_virtual / avg_platform - 1) * 100
            report.append(f"  Virtual threads vs Platform threads: {virtual_improvement:+.1f}% improvement")

        if coroutine_tests:
            avg_coroutine = sum(t['requests_per_second'] for t in coroutine_tests.values()) / len(coroutine_tests)
            if platform_tests:
                avg_platform = sum(t['requests_per_second'] for t in platform_tests.values()) / len(platform_tests)
                coroutine_improvement = (avg_coroutine / avg_platform - 1) * 100
                report.append(f"  Kotlin coroutines vs Platform threads: {coroutine_improvement:+.1f}% improvement")

        return "\n".join(report)

    def export_to_csv(self, filename="performance_comparison.csv"):
        """Export results to CSV for further analysis"""
        if not self.tests:
            return

        fieldnames = ['test_name', 'requests_per_second', 'time_per_request_mean',
                      'time_per_request_concurrent', 'failed_requests', 'total_requests',
                      'concurrency_level', 'p50', 'p90', 'p95', 'p99']

        csv_file = self.results_dir / filename
        with open(csv_file, 'w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()

            for test_name, metrics in self.tests.items():
                row = {
                    'test_name': test_name,
                    'requests_per_second': metrics['requests_per_second'],
                    'time_per_request_mean': metrics['time_per_request_mean'],
                    'time_per_request_concurrent': metrics['time_per_request_concurrent'],
                    'failed_requests': metrics['failed_requests'],
                    'total_requests': metrics['total_requests'],
                    'concurrency_level': metrics['concurrency_level']
                }

                # Add percentiles
                percentiles = metrics.get('percentiles', {})
                for p in ['p50', 'p90', 'p95', 'p99']:
                    row[p] = percentiles.get(p, '')

                writer.writerow(row)

        print(f"Results exported to: {csv_file}")

    def export_to_json(self, filename="performance_results.json"):
        """Export results to JSON"""
        json_file = self.results_dir / filename
        with open(json_file, 'w') as f:
            json.dump(self.tests, f, indent=2)
        print(f"Results exported to: {json_file}")

def main():
    analyzer = PerformanceAnalyzer()
    analyzer.load_all_results()

    if not analyzer.tests:
        print("No test results found. Please run the performance tests first.")
        sys.exit(1)

    # Generate and save comparison report
    report = analyzer.generate_comparison_report()
    print(report)

    # Save report to file
    report_file = analyzer.results_dir / f"comparison_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
    with open(report_file, 'w') as f:
        f.write(report)
    print(f"\nReport saved to: {report_file}")

    # Export to CSV and JSON
    analyzer.export_to_csv()
    analyzer.export_to_json()

if __name__ == "__main__":
    main()