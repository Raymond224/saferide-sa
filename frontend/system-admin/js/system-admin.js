function filterUsers(){
  const q=document.getElementById('userSearch')?.value.toLowerCase()||'';
  const role=document.getElementById('roleFilter')?.value||'';
  const status=document.getElementById('statusFilter')?.value||'';
  document.querySelectorAll('#userTable tr').forEach(row=>{
    const text=row.innerText.toLowerCase();
    row.style.display=(!q||text.includes(q))&&(!role||row.dataset.role===role)&&(!status||row.dataset.status===status)?'':'none';
  });
}
function toggleStatus(btn){
  const row=btn.closest('tr');
  const badge=row.querySelector('.badge');
  const suspended=row.dataset.status==='Suspended';
  row.dataset.status=suspended?'Active':'Suspended';
  badge.textContent=suspended?'Active':'Suspended';
  badge.className='badge '+(suspended?'badge-green':'badge-red');
  btn.textContent=suspended?'Suspend':'Activate';
}
function filterSchools(){
  const q=document.getElementById('schoolSearch')?.value.toLowerCase()||'';
  document.querySelectorAll('#schoolTable tr').forEach(row=>{
    row.style.display=row.innerText.toLowerCase().includes(q)?'':'none';
  });
}
function filterAudit(){
  const q=document.getElementById('auditSearch')?.value.toLowerCase()||'';
  const action=document.getElementById('auditAction')?.value||'';
  document.querySelectorAll('#auditTable tr').forEach(row=>{
    const text=row.innerText.toLowerCase();
    const rowAction=row.children[2]?.innerText||'';
    row.style.display=(!q||text.includes(q))&&(!action||rowAction===action)?'':'none';
  });
}


function openAddSchoolForm(){
  document.getElementById('addSchoolModal').style.display = 'flex';
}

function closeAddSchoolForm(){
  document.getElementById('addSchoolModal').style.display = 'none';

  document.getElementById('newSchoolName').value = '';
  document.getElementById('newSchoolAddress').value = '';
  document.getElementById('newSchoolAdmin').value = '';
  document.getElementById('newSchoolContact').value = '';
}

function addSchool(){
  const name = document.getElementById('newSchoolName').value.trim();
  const address = document.getElementById('newSchoolAddress').value.trim();
  const admin = document.getElementById('newSchoolAdmin').value.trim();
  const contact = document.getElementById('newSchoolContact').value.trim();

  if(!name || !address || !admin || !contact){
    alert('Please complete all fields.');
    return;
  }

  const table = document.getElementById('schoolTable');
  const row = document.createElement('tr');

  row.innerHTML = `
    <td>${name}</td>
    <td>${address}</td>
    <td>${admin}</td>
    <td>${contact}</td>
    <td><span class="badge badge-green">Active</span></td>
    <td><button class="btn btn-outline btn-sm">View</button></td>
  `;

  table.appendChild(row);

  closeAddSchoolForm();

  alert('School added successfully.');
}


function viewSchool(button){
  const row = button.closest('tr');
  const cells = row.querySelectorAll('td');

  document.getElementById('viewSchoolName').textContent = cells[0].textContent;
  document.getElementById('viewSchoolAddress').textContent = cells[1].textContent;
  document.getElementById('viewSchoolAdmin').textContent = cells[2].textContent;
  document.getElementById('viewSchoolContact').textContent = cells[3].textContent;
  document.getElementById('viewSchoolStatus').textContent = cells[4].textContent;

  document.getElementById('schoolDetailsModal').style.display = 'flex';
}

function closeSchoolDetails(){
  document.getElementById('schoolDetailsModal').style.display = 'none';
}