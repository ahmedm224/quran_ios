#!/usr/bin/env python3
"""
Standalone script to download all 114 Surahs with Ayah data
from Al-Quran Cloud API

This script can be used for:
1. Testing the API before integrating into the Android app
2. Downloading data for backup/offline use
3. Generating JSON files for manual inspection
"""

import requests
import json
import time
from pathlib import Path
from typing import Dict, List, Any

BASE_URL = "http://api.alquran.cloud/v1"
OUTPUT_DIR = Path("quran_data")
DELAY_BETWEEN_REQUESTS = 0.1  # seconds


def download_surah(surah_number: int) -> Dict[str, Any]:
    """Download a single Surah with all its Ayahs"""
    url = f"{BASE_URL}/surah/{surah_number}"
    print(f"Downloading Surah {surah_number}/114...", end=" ")

    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        data = response.json()

        if data.get("code") == 200 and data.get("status") == "OK":
            surah_data = data["data"]
            print(f"OK - {surah_data['englishName']} ({surah_data['numberOfAyahs']} ayahs)")
            return surah_data
        else:
            print(f"FAILED: {data.get('status')}")
            return None
    except Exception as e:
        print(f"ERROR: {e}")
        return None


def download_all_surahs() -> List[Dict[str, Any]]:
    """Download all 114 Surahs"""
    all_surahs = []

    for surah_number in range(1, 115):
        surah_data = download_surah(surah_number)

        if surah_data:
            all_surahs.append(surah_data)

        # Delay to avoid overwhelming the API
        time.sleep(DELAY_BETWEEN_REQUESTS)

    return all_surahs


def save_to_json(surahs: List[Dict[str, Any]]):
    """Save downloaded data to JSON files"""
    OUTPUT_DIR.mkdir(exist_ok=True)

    # Save complete data
    complete_file = OUTPUT_DIR / "complete_quran.json"
    with open(complete_file, "w", encoding="utf-8") as f:
        json.dump(surahs, f, ensure_ascii=False, indent=2)
    print(f"\n[OK] Saved complete Quran data to: {complete_file}")

    # Save individual Surahs
    surahs_dir = OUTPUT_DIR / "surahs"
    surahs_dir.mkdir(exist_ok=True)

    for surah in surahs:
        surah_file = surahs_dir / f"surah_{surah['number']:03d}.json"
        with open(surah_file, "w", encoding="utf-8") as f:
            json.dump(surah, f, ensure_ascii=False, indent=2)

    print(f"[OK] Saved {len(surahs)} individual Surah files to: {surahs_dir}")


def generate_statistics(surahs: List[Dict[str, Any]]):
    """Generate and display statistics"""
    total_ayahs = sum(s["numberOfAyahs"] for s in surahs)
    meccan_count = sum(1 for s in surahs if s["revelationType"] == "Meccan")
    medinan_count = len(surahs) - meccan_count

    print("\n" + "="*60)
    print("QURAN DATA STATISTICS")
    print("="*60)
    print(f"Total Surahs:    {len(surahs)}")
    print(f"Total Ayahs:     {total_ayahs}")
    print(f"Meccan Surahs:   {meccan_count}")
    print(f"Medinan Surahs:  {medinan_count}")
    print("="*60)

    # Save statistics
    stats = {
        "total_surahs": len(surahs),
        "total_ayahs": total_ayahs,
        "meccan_surahs": meccan_count,
        "medinan_surahs": medinan_count,
        "surahs": [
            {
                "number": s["number"],
                "name": s["name"],
                "englishName": s["englishName"],
                "numberOfAyahs": s["numberOfAyahs"],
                "revelationType": s["revelationType"]
            }
            for s in surahs
        ]
    }

    stats_file = OUTPUT_DIR / "statistics.json"
    with open(stats_file, "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print(f"\n[OK] Saved statistics to: {stats_file}")


def generate_csv(surahs: List[Dict[str, Any]]):
    """Generate CSV file with all Ayahs"""
    csv_file = OUTPUT_DIR / "all_ayahs.csv"

    with open(csv_file, "w", encoding="utf-8") as f:
        # Header
        f.write("surah_number,surah_name_arabic,surah_name_english,ayah_number,global_ayah_number,text_arabic,juz,manzil,page,ruku,hizb_quarter,sajda\n")

        # Data
        for surah in surahs:
            for ayah in surah["ayahs"]:
                sajda = "true" if ayah.get("sajda", False) is True else "false"
                line = f'{surah["number"]},"{surah["name"]}","{surah["englishName"]}",{ayah["numberInSurah"]},{ayah["number"]},"{ayah["text"]}",{ayah["juz"]},{ayah["manzil"]},{ayah["page"]},{ayah["ruku"]},{ayah["hizbQuarter"]},{sajda}\n'
                f.write(line)

    print(f"[OK] Saved CSV file to: {csv_file}")


def main():
    print("="*60)
    print("QURAN DATA DOWNLOADER")
    print("Downloading from Al-Quran Cloud API")
    print("="*60)
    print()

    # Download all Surahs
    surahs = download_all_surahs()

    if not surahs:
        print("\n[ERROR] Failed to download any data")
        return

    # Save data
    save_to_json(surahs)
    generate_csv(surahs)
    generate_statistics(surahs)

    print("\n[SUCCESS] All done!")


if __name__ == "__main__":
    main()
