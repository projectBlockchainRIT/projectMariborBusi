#!/usr/bin/env python3
"""
Master script - zažene celoten pipeline projekta avtomatično
1. Kompaјlira C++ simulator
2. Zažene simulator za generiranje podatkov
3. Pripravi treningske podatke
4. Trenira nevronsko mrežo
5. Odpre interaktivno vizualizacijo
"""

import subprocess
import sys
import time
import os
from pathlib import Path

class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_step(step_num, title):
    """Izpiši naslov koraka"""
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*70}")
    print(f"KORAK {step_num}: {title}")
    print(f"{'='*70}{Colors.ENDC}\n")

def run_command(cmd, description, capture_input=None):
    """Zaženi command in preveri če je uspel"""
    print(f"{Colors.OKCYAN}🔄 {description}...{Colors.ENDC}")
    
    try:
        if capture_input:
            # Za interaktivne programe
            process = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                shell=isinstance(cmd, str)
            )
            stdout, stderr = process.communicate(input=capture_input)
            print(stdout)
            if stderr:
                print(f"{Colors.WARNING}{stderr}{Colors.ENDC}")
            
            if process.returncode != 0:
                raise subprocess.CalledProcessError(process.returncode, cmd)
        else:
            # Za navadne programe
            result = subprocess.run(
                cmd,
                shell=isinstance(cmd, str),
                check=True,
                capture_output=False,
                text=True
            )
        
        print(f"{Colors.OKGREEN}✅ {description} - uspešno!{Colors.ENDC}")
        return True
    
    except subprocess.CalledProcessError as e:
        print(f"{Colors.FAIL}❌ {description} - neuspešno!{Colors.ENDC}")
        print(f"{Colors.FAIL}Napaka: {e}{Colors.ENDC}")
        return False
    except Exception as e:
        print(f"{Colors.FAIL}❌ Neznana napaka: {e}{Colors.ENDC}")
        return False

def check_file_exists(filepath, description):
    """Preveri če datoteka obstaja"""
    if Path(filepath).exists():
        print(f"{Colors.OKGREEN}✅ {description} obstaja{Colors.ENDC}")
        return True
    else:
        print(f"{Colors.FAIL}❌ {description} ne obstaja{Colors.ENDC}")
        return False

def main():
    print(f"{Colors.HEADER}{Colors.BOLD}")
    print("=" * 70)
    print("🚀 AVTOMATSKI PIPELINE - BUS LOCATION PREDICTION")
    print("=" * 70)
    print(f"{Colors.ENDC}")
    
    # Preveri ali smo v pravilnem direktoriju
    if not Path("routes.json").exists():
        print(f"{Colors.FAIL}❌ routes.json ne obstaja! Zagoni iz pravega direktorija.{Colors.ENDC}")
        sys.exit(1)
    
    # Vprašaj uporabnika za parametre
    print(f"\n{Colors.OKBLUE}📋 Konfiguracijske možnosti:{Colors.ENDC}")
    route = input(f"{Colors.BOLD}Izberi linijo (npr. G1): {Colors.ENDC}").strip().upper() or "G1"

    use_generator = input(f"{Colors.BOLD}Uporabi Python generator (hitreje)? (y/n) [y]: {Colors.ENDC}").strip().lower()
    use_generator = use_generator != 'n'

    epochs = input(f"{Colors.BOLD}Število epoch za treniranje [50]: {Colors.ENDC}").strip() or "50"

    print(f"\n{Colors.OKGREEN}✅ Nastavitve:{Colors.ENDC}")
    print(f"   • Linija: {route}")
    print(f"   • Generator: {'Python (multibus, 3 avtobusi)' if use_generator else 'C++ (legacy)'}")
    print(f"   • Epochs: {epochs}")
    
    input(f"\n{Colors.WARNING}Pritisni Enter za začetek...{Colors.ENDC}")
    
    # =====================================================================
    # KORAK 1: Generiranje podatkov
    # =====================================================================
    print_step(1, "GENERIRANJE SIMULACIJSKIH PODATKOV")

    if use_generator:
        # Python generator (faster, multi-bus)
        if not check_file_exists("generate_multibus.py", "generate_multibus.py"):
            print(f"{Colors.FAIL}❌ generate_multibus.py ne obstaja!{Colors.ENDC}")
            sys.exit(1)

        gen_cmd = ["python3", "generate_multibus.py", route]
        if not run_command(gen_cmd, f"Generiranje podatkov za {route} (3 avtobusi)"):
            sys.exit(1)
    else:
        # C++ simulator (legacy)
        print(f"{Colors.OKCYAN}🔨 Kompaјliram C++ simulator...{Colors.ENDC}")

        if not check_file_exists("main_v2.cpp", "main_v2.cpp"):
            print(f"{Colors.FAIL}❌ main_v2.cpp ne obstaja!{Colors.ENDC}")
            sys.exit(1)

        compile_cmd = ["clang++", "-std=c++17", "-o", "main_v2", "main_v2.cpp"]
        if not run_command(compile_cmd, "Kompaјliranje main_v2.cpp"):
            sys.exit(1)

        if not run_command("./main_v2", f"Simulacija linije {route}", capture_input=f"{route}\n"):
            sys.exit(1)
    
    time.sleep(2)

    # =====================================================================
    # KORAK 2: Priprava treningskih podatkov
    # =====================================================================
    print_step(2, "PRIPRAVA TRENINGSKIH PODATKOV")
    
    if not check_file_exists("prepare_training_data.py", "prepare_training_data.py"):
        print(f"{Colors.FAIL}❌ prepare_training_data.py ne obstaja!{Colors.ENDC}")
        sys.exit(1)
    
    prep_cmd = ["python3", "prepare_training_data.py"]
    if not run_command(prep_cmd, "Priprava treningskih podatkov", capture_input=f"{route}\n"):
        sys.exit(1)
    
    time.sleep(2)

    # =====================================================================
    # KORAK 3: Treniranje nevronske mreže
    # =====================================================================
    print_step(3, "TRENIRANJE NEVRONSKE MREŽE")
    
    if not check_file_exists("train_model.py", "train_model.py"):
        print(f"{Colors.FAIL}❌ train_model.py ne obstaja!{Colors.ENDC}")
        sys.exit(1)
    
    train_cmd = ["python3", "train_model.py"]
    if not run_command(train_cmd, f"Treniranje modela ({epochs} epoch)", capture_input=f"{route}\n"):
        sys.exit(1)
    
    time.sleep(2)

    # =====================================================================
    # KORAK 4: Interaktivna vizualizacija
    # =====================================================================
    print_step(4, "INTERAKTIVNA VIZUALIZACIJA")
    
    print(f"{Colors.OKBLUE}📊 Odpiram interaktivno vizualizacijo...{Colors.ENDC}")
    print(f"{Colors.WARNING}Uporabi slider za premikanje skozi čas!{Colors.ENDC}")
    print(f"{Colors.WARNING}Zapri okno za konec.{Colors.ENDC}\n")
    
    time.sleep(1)
    
    if not check_file_exists("interactive_viz.py", "interactive_viz.py"):
        print(f"{Colors.FAIL}❌ interactive_viz.py ne obstaja!{Colors.ENDC}")
        sys.exit(1)
    
    viz_cmd = ["python3", "interactive_viz.py"]
    run_command(viz_cmd, "Interaktivna vizualizacija", capture_input=f"{route}\n")
    
    # =====================================================================
    # ZAKLJUČEK
    # =====================================================================
    print(f"\n{Colors.HEADER}{Colors.BOLD}")
    print("=" * 70)
    print("🎉 VSE KORAKE USPEŠNO ZAKLJUČENI!")
    print("=" * 70)
    print(f"{Colors.ENDC}")
    
    print(f"\n{Colors.OKGREEN}📁 Generirani podatki:{Colors.ENDC}")
    data_type = "multibus" if use_generator else "all"
    print(f"   • Simulacija: data/{route}_{data_type}_*.csv")
    print(f"   • Treningski podatki: training_data/{route}_training_*")
    print(f"   • Model: results/{route}_model.h5")
    print(f"   • Vizualizacije: results/{route}_*.png")
    
    print(f"\n{Colors.OKBLUE}💡 Naslednji koraki:{Colors.ENDC}")
    print(f"   • Ponovno zaženi visualizacijo: python3 interactive_viz.py")
    print(f"   • Prikaži rezultate: odprи results/{route}_predictions.png")
    print(f"   • Statistike: odprи data/{route}_statistics.png")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(f"\n\n{Colors.WARNING}⚠️  Pipeline prekinjen s strani uporabnika{Colors.ENDC}")
        sys.exit(0)
    except Exception as e:
        print(f"\n{Colors.FAIL}❌ Napaka: {e}{Colors.ENDC}")
        sys.exit(1)
