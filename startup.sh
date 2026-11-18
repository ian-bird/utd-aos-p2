#!/usr/bin/env bash
set -euo pipefail

# Compile the code
cd project/p2
make build

echo "im here 1"

mkdir -p machine-outputs


echo "im here 2"


# (This relies on my ~/.ssh/config!)
#MACHINES=csa-dc{01..45} | grep -v csa-dc07 # Skip dc07 since it seems down?
MACHINES=(csa-dc01 csa-dc02 csa-dc03 csa-dc04 csa-dc05  csa-dc06  csa-dc08  csa-dc09  csa-dc10  csa-dc11  csa-dc12  csa-dc13  csa-dc14  csa-dc15  csa-dc16  csa-dc17  csa-dc18  csa-dc19  csa-dc20  csa-dc21  csa-dc22  csa-dc23  csa-dc24  csa-dc25  csa-dc26  csa-dc27  csa-dc28  csa-dc29  csa-dc30  csa-dc31  csa-dc32  csa-dc33  csa-dc34  csa-dc35  csa-dc36  csa-dc37  csa-dc38  csa-dc39  csa-dc40  csa-dc41  csa-dc42  csa-dc43  csa-dc44  csa-dc45)

echo "im here 3"


for ip in "${MACHINES[@]}"; do
    echo $ip
# for ip in dc{01..06}; do
    ssh $ip -q 'cd src/utd-aos-p2/project/p2; make run' &
    #ssh $ip -q 'pwd' > ./machine-outputs/$ip.txt &
done


echo "im here 4"


wait # Blocks until all background jobs are done

echo "All ssh jobs are done! Check machine-outputs/"

# no need for cleanup :)
# read -p "Press enter to continue"
# read -p "Would you like to cleanup? Hit enter to continue or Ctrl-C to cancel"


# for ip in $MACHINES; do
#     ssh $ip -q 'killall ' &
# done
