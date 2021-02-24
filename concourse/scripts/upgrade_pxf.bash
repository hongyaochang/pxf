#!/bin/bash

set -euxo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

GPHOME=/usr/local/greenplum-db-devel
# whether PXF is being installed from a new component-based packaging
PXF_COMPONENT=${PXF_COMPONENT:=false}
if [[ ${PXF_COMPONENT} == "true" ]]; then
	PXF5_HOME=/usr/local/pxf-gp${GP_VER}
else
	PXF5_HOME=${GPHOME}/pxf
fi

# we need word boundary in case of standby master (smdw)
MASTER_HOSTNAME=$(grep < cluster_env_files/etc_hostfile '\bmdw' | awk '{print $2}')
PXF_HOME=/usr/local/pxf-gp${GP_VER}
PXF_BASE_DIR=${PXF_BASE_DIR:-$PXF_HOME}

echoGreen() { echo $'\e[0;32m'"$1"$'\e[0m'; }

function upgrade_pxf() {
	echoGreen "Stopping PXF 5"
	ssh "${MASTER_HOSTNAME}" "${PXF5_HOME}/bin/pxf version && ${PXF5_HOME}/bin/pxf cluster stop"

	echoGreen "Installing PXF 6"
	ssh "${MASTER_HOSTNAME}" "
		source ${GPHOME}/greenplum_path.sh &&
		export JAVA_HOME=/usr/lib/jvm/jre &&
		gpscp -f ~gpadmin/hostfile_all -v -u centos -r ~/pxf_tarball centos@=: &&
		gpssh -f ~gpadmin/hostfile_all -v -u centos -s -e 'tar -xzf ~centos/pxf_tarball/pxf-*.tar.gz -C /tmp' &&
		gpssh -f ~gpadmin/hostfile_all -v -u centos -s -e 'sudo GPHOME=${GPHOME} /tmp/pxf*/install_component'
	"

	echoGreen "Change ownership of PXF 6 directory to gpadmin"
	ssh "${MASTER_HOSTNAME}" "source ${GPHOME}/greenplum_path.sh && gpssh -f ~gpadmin/hostfile_all -v -u centos -s -e 'sudo chown -R gpadmin:gpadmin ${PXF_HOME}'"

	echoGreen "Check the PXF 6 version"
	ssh "${MASTER_HOSTNAME}" "${PXF_HOME}/bin/pxf version"

	echoGreen "Register the PXF extension into Greenplum"
	ssh "${MASTER_HOSTNAME}" "GPHOME=${GPHOME} ${PXF_HOME}/bin/pxf cluster register"

	if [[ "${PXF_BASE_DIR}" != "${PXF_HOME}" ]]; then
		echoGreen "Prepare PXF in ${PXF_BASE_DIR}"
		ssh "${MASTER_HOSTNAME}" "
			PXF_BASE=${PXF_BASE_DIR} ${PXF_HOME}/bin/pxf cluster prepare
			echo \"export PXF_BASE=${PXF_BASE_DIR}\" >> ~gpadmin/.bashrc
		"
	fi

	echoGreen "Perform PXF migrate from PXF_CONF=~gpadmin/pxf to PXF_BASE_DIR=${PXF_BASE_DIR}"
	ssh "${MASTER_HOSTNAME}" "PXF_BASE=${PXF_BASE_DIR} PXF_CONF=~gpadmin/pxf ${PXF_HOME}/bin/pxf cluster migrate"
	
	echoGreen "Update pxf.conf to pxf.base in pxf-site.xml files"
	ssh "${MASTER_HOSTNAME}" "find ${PXF_BASE_DIR} -name pxf-site.xml -exec sed -i s/pxf.conf/pxf.base/g {} +"

	echoGreen "Sync updates"
	ssh "${MASTER_HOSTNAME}" "PXF_BASE=${PXF_BASE_DIR} ${PXF_HOME}/bin/pxf cluster sync"

	echoGreen "Starting PXF 6"
	ssh "${MASTER_HOSTNAME}" "PXF_BASE=${PXF_BASE_DIR} ${PXF_HOME}/bin/pxf cluster start"

	echoGreen "ALTER EXTENSION pxf UPDATE - for pxfautomation database"
	ssh "${MASTER_HOSTNAME}" "source ${GPHOME}/greenplum_path.sh && psql -d pxfautomation -c 'ALTER EXTENSION pxf UPDATE'"
}

function _main() {
	scp -r pxf_tarball "${MASTER_HOSTNAME}:~gpadmin"
	upgrade_pxf
}

_main
