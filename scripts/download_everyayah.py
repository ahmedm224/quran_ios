#!/usr/bin/env python3
"""
EveryAyah.com Audio Downloader for Quran Media App (Alfurqan)

Downloads all ayah-by-ayah audio files for the reciters used in the app.
Organized for direct migration to Cloudflare R2 or other CDN.

Directory Structure (matches EveryAyah.com):
    {output}/{folder}/{surah3}{ayah3}.mp3

Examples:
    {output}/Minshawy_Murattal_128kbps/001001.mp3  (Surah 1, Ayah 1)
    {output}/Minshawy_Murattal_128kbps/002286.mp3  (Surah 2, Ayah 286)
    {output}/warsh/warsh_Abdul_Basit_128kbps/001001.mp3  (Warsh recitation)

Usage:
    python download_everyayah.py --output /path/to/output/folder
    python download_everyayah.py --output /path/to/output --reciters "Minshawy_Murattal_128kbps,Husary_128kbps"
    python download_everyayah.py --output /path/to/output --workers 10
    python download_everyayah.py --list-reciters

Total: 44 reciters, 6236 ayahs each = ~274,384 files
Estimated size: ~66 GB (varies by bitrate)

Note: This downloads ONLY the reciters enabled in the app.
      Excluded reciters (Alafasy, As-Sudais) are from Al-Quran Cloud API.
"""

import os
import sys
import argparse
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import time
import hashlib
import json
from datetime import datetime

# ============================================================================
# RECITER CONFIGURATION - Matches app's ReciterDataPopulatorWorker.kt
# ============================================================================
RECITERS = [
    {"folder": "Abdul_Basit_Murattal_192kbps", "name": "Abdul Basit Abdul Samad", "style": "Murattal"},
    {"folder": "Abdul_Basit_Mujawwad_128kbps", "name": "Abdul Basit Abdul Samad", "style": "Mujawwad"},
    {"folder": "Husary_128kbps", "name": "Mahmoud Khalil Al-Hussary", "style": "Murattal"},
    {"folder": "Husary_Mujawwad_64kbps", "name": "Mahmoud Khalil Al-Hussary", "style": "Mujawwad"},
    {"folder": "Minshawy_Murattal_128kbps", "name": "Mohamed Siddiq Al-Minshawi", "style": "Murattal"},
    {"folder": "Minshawy_Mujawwad_64kbps", "name": "Mohamed Siddiq Al-Minshawi", "style": "Mujawwad"},
    {"folder": "Ghamadi_40kbps", "name": "Saad Al-Ghamdi", "style": "Murattal"},
    {"folder": "MaherAlMuaiqly128kbps", "name": "Maher Al-Muaiqly", "style": "Murattal"},
    {"folder": "Muhammad_Ayyoub_128kbps", "name": "Muhammad Ayyub", "style": "Murattal"},
    {"folder": "Abu_Bakr_Ash-Shaatree_128kbps", "name": "Abu Bakr Al-Shatri", "style": "Murattal"},
    {"folder": "Hani_Rifai_192kbps", "name": "Hani Ar-Rifai", "style": "Murattal"},
    {"folder": "ahmed_ibn_ali_al_ajamy_128kbps", "name": "Ahmed ibn Ali al-Ajamy", "style": "Murattal"},
    {"folder": "Nasser_Alqatami_128kbps", "name": "Nasser Al-Qatami", "style": "Murattal"},
    {"folder": "Mohammad_al_Tablaway_128kbps", "name": "Mohammad al-Tablaway", "style": "Murattal"},
    {"folder": "Mustafa_Ismail_48kbps", "name": "Mustafa Ismail", "style": "Mujawwad"},
    {"folder": "Salaah_AbdulRahman_Bukhatir_128kbps", "name": "Salaah AbdulRahman Bukhatir", "style": "Murattal"},
    {"folder": "Muhsin_Al_Qasim_192kbps", "name": "Muhsin Al-Qasim", "style": "Murattal"},
    {"folder": "Abdullaah_3awwaad_Al-Juhaynee_128kbps", "name": "Abdullah Awad Al-Juhani", "style": "Murattal"},
    {"folder": "Salah_Al_Budair_128kbps", "name": "Salah Al-Budair", "style": "Murattal"},
    {"folder": "Abdullah_Matroud_128kbps", "name": "Abdullah Matroud", "style": "Murattal"},
    {"folder": "Ahmed_Neana_128kbps", "name": "Ahmed Neana", "style": "Murattal"},
    {"folder": "Muhammad_AbdulKareem_128kbps", "name": "Muhammad AbdulKareem", "style": "Murattal"},
    {"folder": "khalefa_al_tunaiji_64kbps", "name": "Khalefa Al-Tunaiji", "style": "Murattal"},
    {"folder": "mahmoud_ali_al_banna_32kbps", "name": "Mahmoud Ali Al-Banna", "style": "Mujawwad"},
    {"folder": "Khaalid_Abdullaah_al-Qahtaanee_192kbps", "name": "Khalid Abdullah Al-Qahtani", "style": "Murattal"},
    {"folder": "Yasser_Ad-Dussary_128kbps", "name": "Yasser Ad-Dussary", "style": "Murattal"},
    {"folder": "Ali_Hajjaj_AlSuesy_128kbps", "name": "Ali Hajjaj Al-Suesy", "style": "Murattal"},
    {"folder": "Sahl_Yassin_128kbps", "name": "Sahl Yassin", "style": "Murattal"},
    {"folder": "aziz_alili_128kbps", "name": "Aziz Alili", "style": "Murattal"},
    {"folder": "Yaser_Salamah_128kbps", "name": "Yaser Salamah", "style": "Murattal"},
    {"folder": "Akram_AlAlaqimy_128kbps", "name": "Akram Al-Alaqimy", "style": "Murattal"},
    {"folder": "Ali_Jaber_64kbps", "name": "Ali Jaber", "style": "Murattal"},
    {"folder": "Fares_Abbad_64kbps", "name": "Fares Abbad", "style": "Murattal"},
    {"folder": "Ayman_Sowaid_64kbps", "name": "Ayman Sowaid", "style": "Murattal"},
    {"folder": "Ibrahim_Akhdar_32kbps", "name": "Ibrahim Al-Akhdar", "style": "Murattal"},
    {"folder": "Parhizgar_48kbps", "name": "Shahriar Parhizgar", "style": "Murattal"},
    {"folder": "Saood_ash-Shuraym_128kbps", "name": "Saud Ash-Shuraim", "style": "Murattal"},
    {"folder": "Abdullah_Basfar_192kbps", "name": "Abdullah Basfar", "style": "Murattal"},
    {"folder": "Hudhaify_128kbps", "name": "Ali Al-Hudhaify", "style": "Murattal"},
    {"folder": "Muhammad_Jibreel_128kbps", "name": "Muhammad Jibreel", "style": "Murattal"},
    {"folder": "warsh/warsh_Abdul_Basit_128kbps", "name": "Abdul Basit (Warsh)", "style": "Warsh"},
    {"folder": "warsh/warsh_ibrahim_aldosary_128kbps", "name": "Ibrahim Ad-Dosari (Warsh)", "style": "Warsh"},
    {"folder": "Karim_Mansoori_40kbps", "name": "Karim Mansoori", "style": "Murattal"},
    {"folder": "warsh/warsh_yassin_al_jazaery_64kbps", "name": "Yassin Al-Jazaery", "style": "Warsh"},
]

# ============================================================================
# SURAH AYAH COUNTS - Standard Quran structure (114 surahs, 6236 total ayahs)
# ============================================================================
SURAH_AYAH_COUNTS = [
    7,    # 1. Al-Fatiha
    286,  # 2. Al-Baqarah
    200,  # 3. Aal-E-Imran
    176,  # 4. An-Nisa
    120,  # 5. Al-Ma'idah
    165,  # 6. Al-An'am
    206,  # 7. Al-A'raf
    75,   # 8. Al-Anfal
    129,  # 9. At-Tawbah
    109,  # 10. Yunus
    123,  # 11. Hud
    111,  # 12. Yusuf
    43,   # 13. Ar-Ra'd
    52,   # 14. Ibrahim
    99,   # 15. Al-Hijr
    128,  # 16. An-Nahl
    111,  # 17. Al-Isra
    110,  # 18. Al-Kahf
    98,   # 19. Maryam
    135,  # 20. Ta-Ha
    112,  # 21. Al-Anbiya
    78,   # 22. Al-Hajj
    118,  # 23. Al-Mu'minun
    64,   # 24. An-Nur
    77,   # 25. Al-Furqan
    227,  # 26. Ash-Shu'ara
    93,   # 27. An-Naml
    88,   # 28. Al-Qasas
    69,   # 29. Al-Ankabut
    60,   # 30. Ar-Rum
    34,   # 31. Luqman
    30,   # 32. As-Sajdah
    73,   # 33. Al-Ahzab
    54,   # 34. Saba
    45,   # 35. Fatir
    83,   # 36. Ya-Sin
    182,  # 37. As-Saffat
    88,   # 38. Sad
    75,   # 39. Az-Zumar
    85,   # 40. Ghafir
    54,   # 41. Fussilat
    53,   # 42. Ash-Shura
    89,   # 43. Az-Zukhruf
    59,   # 44. Ad-Dukhan
    37,   # 45. Al-Jathiyah
    35,   # 46. Al-Ahqaf
    38,   # 47. Muhammad
    29,   # 48. Al-Fath
    18,   # 49. Al-Hujurat
    45,   # 50. Qaf
    60,   # 51. Adh-Dhariyat
    49,   # 52. At-Tur
    62,   # 53. An-Najm
    55,   # 54. Al-Qamar
    78,   # 55. Ar-Rahman
    96,   # 56. Al-Waqi'ah
    29,   # 57. Al-Hadid
    22,   # 58. Al-Mujadila
    24,   # 59. Al-Hashr
    13,   # 60. Al-Mumtahanah
    14,   # 61. As-Saff
    11,   # 62. Al-Jumu'ah
    11,   # 63. Al-Munafiqun
    18,   # 64. At-Taghabun
    12,   # 65. At-Talaq
    12,   # 66. At-Tahrim
    30,   # 67. Al-Mulk
    52,   # 68. Al-Qalam
    52,   # 69. Al-Haqqah
    44,   # 70. Al-Ma'arij
    28,   # 71. Nuh
    28,   # 72. Al-Jinn
    20,   # 73. Al-Muzzammil
    56,   # 74. Al-Muddaththir
    40,   # 75. Al-Qiyamah
    31,   # 76. Al-Insan
    50,   # 77. Al-Mursalat
    40,   # 78. An-Naba
    46,   # 79. An-Nazi'at
    42,   # 80. Abasa
    29,   # 81. At-Takwir
    19,   # 82. Al-Infitar
    36,   # 83. Al-Mutaffifin
    25,   # 84. Al-Inshiqaq
    22,   # 85. Al-Buruj
    17,   # 86. At-Tariq
    19,   # 87. Al-A'la
    26,   # 88. Al-Ghashiyah
    30,   # 89. Al-Fajr
    20,   # 90. Al-Balad
    15,   # 91. Ash-Shams
    21,   # 92. Al-Layl
    11,   # 93. Ad-Duhaa
    8,    # 94. Ash-Sharh
    8,    # 95. At-Tin
    19,   # 96. Al-Alaq
    5,    # 97. Al-Qadr
    8,    # 98. Al-Bayyinah
    8,    # 99. Az-Zalzalah
    11,   # 100. Al-Adiyat
    11,   # 101. Al-Qari'ah
    8,    # 102. At-Takathur
    3,    # 103. Al-Asr
    9,    # 104. Al-Humazah
    5,    # 105. Al-Fil
    4,    # 106. Quraysh
    7,    # 107. Al-Ma'un
    3,    # 108. Al-Kawthar
    6,    # 109. Al-Kafirun
    3,    # 110. An-Nasr
    5,    # 111. Al-Masad
    4,    # 112. Al-Ikhlas
    5,    # 113. Al-Falaq
    6,    # 114. An-Nas
]

BASE_URL = "https://everyayah.com/data"


class DownloadStats:
    """Track download statistics"""
    def __init__(self):
        self.total = 0
        self.downloaded = 0
        self.skipped = 0
        self.failed = 0
        self.verified = 0
        self.failed_files = []
        self.start_time = None

    def start(self, total):
        self.total = total
        self.start_time = time.time()

    def elapsed(self):
        if self.start_time:
            return time.time() - self.start_time
        return 0

    def progress_str(self):
        done = self.downloaded + self.skipped + self.failed
        pct = (done / self.total * 100) if self.total > 0 else 0
        elapsed = self.elapsed()
        rate = done / elapsed if elapsed > 0 else 0
        eta = (self.total - done) / rate if rate > 0 else 0
        return f"[{done}/{self.total}] {pct:.1f}% | New: {self.downloaded} | Exists: {self.skipped} | Failed: {self.failed} | ETA: {eta/60:.1f}min"

    def save_progress(self, output_dir: Path):
        """Save progress to file for resume capability"""
        progress_file = output_dir / ".download_progress.json"
        progress = {
            "timestamp": datetime.now().isoformat(),
            "total": self.total,
            "downloaded": self.downloaded,
            "skipped": self.skipped,
            "failed": self.failed,
            "failed_files": self.failed_files
        }
        with open(progress_file, 'w') as f:
            json.dump(progress, f, indent=2)

    @staticmethod
    def load_progress(output_dir: Path):
        """Load previous progress if exists"""
        progress_file = output_dir / ".download_progress.json"
        if progress_file.exists():
            try:
                with open(progress_file, 'r') as f:
                    return json.load(f)
            except:
                pass
        return None


# Minimum file size to consider valid (1KB) - most ayah files are larger
MIN_VALID_FILE_SIZE = 1024


def download_file(url: str, output_path: Path, session: requests.Session, retries: int = 3, force: bool = False) -> tuple:
    """
    Download a single file with retry logic.
    Returns (success: bool, skipped: bool, error: str or None)

    Files are skipped if they already exist with valid size (>1KB).
    This allows safe resume after interruption.
    """
    # Skip if already exists with valid content (not corrupted/partial)
    if not force and output_path.exists():
        file_size = output_path.stat().st_size
        if file_size >= MIN_VALID_FILE_SIZE:
            return (True, True, None)  # Valid file exists, skip
        else:
            # File exists but too small (likely corrupted/partial), re-download
            try:
                output_path.unlink()
            except:
                pass

    # Create parent directory if needed
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # Use temp file to avoid partial downloads being considered valid
    temp_path = output_path.with_suffix('.tmp')

    for attempt in range(retries):
        try:
            response = session.get(url, timeout=30, stream=True)

            if response.status_code == 200:
                with open(temp_path, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)

                # Verify downloaded file is valid
                if temp_path.stat().st_size >= MIN_VALID_FILE_SIZE:
                    temp_path.rename(output_path)
                    return (True, False, None)
                else:
                    temp_path.unlink()
                    return (False, False, "Downloaded file too small")

            elif response.status_code == 404:
                return (False, False, f"404 Not Found")
            else:
                if attempt < retries - 1:
                    time.sleep(1 * (attempt + 1))
                    continue
                return (False, False, f"HTTP {response.status_code}")

        except requests.exceptions.Timeout:
            if attempt < retries - 1:
                time.sleep(2 * (attempt + 1))
                continue
            return (False, False, "Timeout")
        except requests.exceptions.RequestException as e:
            if attempt < retries - 1:
                time.sleep(2 * (attempt + 1))
                continue
            return (False, False, str(e))
        finally:
            # Clean up temp file on failure
            if temp_path.exists():
                try:
                    temp_path.unlink()
                except:
                    pass

    return (False, False, "Max retries exceeded")


def generate_download_tasks(reciters: list, output_dir: Path) -> list:
    """
    Generate list of download tasks for all files.

    URL Pattern: https://everyayah.com/data/{folder}/{surah3}{ayah3}.mp3
    Output Pattern: {output_dir}/{folder}/{surah3}{ayah3}.mp3

    For R2 migration, just upload the output folder contents and update
    the app's BASE_URL from everyayah.com/data to your R2 domain.
    """
    tasks = []

    for reciter in reciters:
        folder = reciter["folder"]

        for surah_num in range(1, 115):  # 1-114
            ayah_count = SURAH_AYAH_COUNTS[surah_num - 1]

            for ayah_num in range(1, ayah_count + 1):
                surah_str = str(surah_num).zfill(3)
                ayah_str = str(ayah_num).zfill(3)
                filename = f"{surah_str}{ayah_str}.mp3"

                url = f"{BASE_URL}/{folder}/{filename}"
                output_path = output_dir / folder / filename

                tasks.append((url, output_path, reciter["name"], surah_num, ayah_num))

    return tasks


def download_worker(task: tuple, session: requests.Session, stats: DownloadStats, force: bool = False) -> tuple:
    """Worker function for thread pool"""
    url, output_path, reciter_name, surah, ayah = task
    success, skipped, error = download_file(url, output_path, session, force=force)

    if success:
        if skipped:
            stats.skipped += 1
        else:
            stats.downloaded += 1
    else:
        stats.failed += 1
        stats.failed_files.append({
            "url": url,
            "path": str(output_path),
            "reciter": reciter_name,
            "surah": surah,
            "ayah": ayah,
            "error": error
        })

    return (url, success, skipped, error)


def main():
    parser = argparse.ArgumentParser(
        description="Download EveryAyah.com Quran audio files for migration to Cloudflare R2",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --output ./quran_audio
  %(prog)s --output ./quran_audio --reciters "Minshawy_Murattal_128kbps,Husary_128kbps"
  %(prog)s --output ./quran_audio --workers 20
  %(prog)s --list-reciters

Resume Support:
  The script automatically resumes interrupted downloads.
  - Existing files (>1KB) are skipped
  - Partial/corrupted files are re-downloaded
  - Progress is saved every 30 seconds
  - Safe to interrupt with Ctrl+C and run again
        """
    )

    parser.add_argument(
        "--output", "-o",
        type=str,
        help="Output directory for downloaded files"
    )

    parser.add_argument(
        "--reciters", "-r",
        type=str,
        help="Comma-separated list of reciter folders to download (default: all)"
    )

    parser.add_argument(
        "--workers", "-w",
        type=int,
        default=5,
        help="Number of concurrent download workers (default: 5)"
    )

    parser.add_argument(
        "--list-reciters",
        action="store_true",
        help="List all available reciters and exit"
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be downloaded without actually downloading"
    )

    parser.add_argument(
        "--force",
        action="store_true",
        help="Force re-download of all files (ignore existing)"
    )

    args = parser.parse_args()

    # List reciters mode
    if args.list_reciters:
        print("\nAvailable Reciters:")
        print("-" * 80)
        for i, r in enumerate(RECITERS, 1):
            print(f"{i:2}. {r['folder']:<50} {r['name']} ({r['style']})")
        print(f"\nTotal: {len(RECITERS)} reciters")
        print(f"Total ayahs per reciter: {sum(SURAH_AYAH_COUNTS)} files")
        print(f"Total files to download: {len(RECITERS) * sum(SURAH_AYAH_COUNTS):,}")
        return 0

    # Require output directory
    if not args.output:
        parser.error("--output is required (use --list-reciters to see available reciters)")

    output_dir = Path(args.output)

    # Filter reciters if specified
    if args.reciters:
        reciter_folders = [r.strip() for r in args.reciters.split(",")]
        selected_reciters = [r for r in RECITERS if r["folder"] in reciter_folders]

        if not selected_reciters:
            print(f"Error: No matching reciters found for: {args.reciters}")
            print("Use --list-reciters to see available options")
            return 1

        reciters = selected_reciters
    else:
        reciters = RECITERS

    # Generate download tasks
    print(f"\nPreparing download tasks...")
    print(f"Output directory: {output_dir.absolute()}")
    print(f"Reciters: {len(reciters)}")
    print(f"Workers: {args.workers}")

    tasks = generate_download_tasks(reciters, output_dir)
    total_files = len(tasks)

    print(f"Total files to process: {total_files:,}")

    if args.dry_run:
        print(f"\n[DRY RUN] Would download {total_files:,} files")
        print("\nFirst 10 files:")
        for url, path, _, _, _ in tasks[:10]:
            print(f"  {url} -> {path}")
        print("...")
        return 0

    # Confirm before starting large download
    total_ayahs = sum(SURAH_AYAH_COUNTS)
    estimated_size_per_reciter_gb = 1.5  # Approximate GB per reciter
    estimated_total_gb = len(reciters) * estimated_size_per_reciter_gb

    print(f"\nEstimated download size: ~{estimated_total_gb:.1f} GB")
    print(f"Files per reciter: {total_ayahs:,}")

    confirm = input("\nProceed with download? [y/N]: ")
    if confirm.lower() != 'y':
        print("Aborted.")
        return 0

    # Create output directory
    output_dir.mkdir(parents=True, exist_ok=True)

    # Check for previous progress
    prev_progress = DownloadStats.load_progress(output_dir)
    if prev_progress and not args.force:
        print(f"\n[RESUME] Found previous download session from {prev_progress.get('timestamp', 'unknown')}")
        print(f"         Previous: {prev_progress.get('downloaded', 0):,} downloaded, {prev_progress.get('skipped', 0):,} skipped, {prev_progress.get('failed', 0):,} failed")
        print(f"         Continuing from where we left off (existing files will be skipped)...")

    if args.force:
        print("\n[FORCE] Re-downloading all files (ignoring existing)")

    # Initialize stats
    stats = DownloadStats()
    stats.start(total_files)

    # Create session with connection pooling
    session = requests.Session()
    adapter = requests.adapters.HTTPAdapter(
        pool_connections=args.workers,
        pool_maxsize=args.workers * 2,
        max_retries=0  # We handle retries ourselves
    )
    session.mount('http://', adapter)
    session.mount('https://', adapter)

    print(f"\nStarting download at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-" * 80)
    print("(Press Ctrl+C to safely interrupt - progress is auto-saved)\n")

    # Download with thread pool
    interrupted = False
    try:
        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            futures = {
                executor.submit(download_worker, task, session, stats, args.force): task
                for task in tasks
            }

            last_print_time = 0
            last_save_time = time.time()
            for future in as_completed(futures):
                try:
                    future.result()
                except Exception as e:
                    print(f"\nWorker error: {e}")

                current_time = time.time()

                # Print progress every 2 seconds
                if current_time - last_print_time >= 2:
                    print(f"\r{stats.progress_str()}", end="", flush=True)
                    last_print_time = current_time

                # Save progress every 30 seconds
                if current_time - last_save_time >= 30:
                    stats.save_progress(output_dir)
                    last_save_time = current_time

        print(f"\r{stats.progress_str()}")

    except KeyboardInterrupt:
        interrupted = True
        print("\n\n[INTERRUPTED] Download interrupted by user!")
        print("              Progress has been saved. Run the same command to resume.")

    # Print summary
    print("\n" + "=" * 80)
    print("DOWNLOAD COMPLETE")
    print("=" * 80)
    print(f"Total files:     {stats.total:,}")
    print(f"Downloaded:      {stats.downloaded:,}")
    print(f"Skipped:         {stats.skipped:,}")
    print(f"Failed:          {stats.failed:,}")
    print(f"Time elapsed:    {stats.elapsed()/60:.1f} minutes")

    # Save failed files report
    if stats.failed_files:
        failed_report_path = output_dir / "failed_downloads.json"
        with open(failed_report_path, 'w') as f:
            json.dump(stats.failed_files, f, indent=2)
        print(f"\nFailed files report saved to: {failed_report_path}")
        print("\nFailed files:")
        for item in stats.failed_files[:20]:
            print(f"  {item['reciter']} - Surah {item['surah']}:{item['ayah']} - {item['error']}")
        if len(stats.failed_files) > 20:
            print(f"  ... and {len(stats.failed_files) - 20} more")

    # Save manifest for R2 upload
    manifest_path = output_dir / "manifest.json"
    manifest = {
        "generated_at": datetime.now().isoformat(),
        "base_url": BASE_URL,
        "reciters": reciters,
        "total_files": stats.total,
        "downloaded": stats.downloaded,
        "skipped": stats.skipped,
        "failed": stats.failed,
        "surah_ayah_counts": SURAH_AYAH_COUNTS
    }
    with open(manifest_path, 'w') as f:
        json.dump(manifest, f, indent=2)
    print(f"\nManifest saved to: {manifest_path}")

    return 0 if stats.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
