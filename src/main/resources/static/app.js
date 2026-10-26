'use strict';

const $ = id => document.getElementById(id);
const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
const formatSize = bytes => bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
const SOCIAL_PLATFORMS = ['website', 'linkedin', 'instagram', 'facebook', 'youtube', 'x'];
const SOCIAL_LABELS = {website:'Website', linkedin:'LinkedIn', instagram:'Instagram', facebook:'Facebook', youtube:'YouTube', x:'X / Twitter'};
const SOCIAL_HOSTS = {
  linkedin:['linkedin.com'], instagram:['instagram.com'],
  facebook:['facebook.com', 'fb.com'], youtube:['youtube.com', 'youtu.be'],
  x:['x.com', 'twitter.com']
};
function validateHttpsUrl(url) {
  if(!url || url.length > 2048 || url !== url.trim() || url.includes('\\') || /[\u0000-\u001f\u007f\s]/.test(url)) return false;
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'https:' && parsed.hostname.includes('.') &&
      !parsed.username && !parsed.password && !parsed.hash &&
      (!parsed.port || parsed.port === '443') && /^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$/i.test(parsed.hostname);
  } catch { return false; }
}
function socialLinks() {
  if (!$('socialEnabled').checked) return [];
  return SOCIAL_PLATFORMS.map(platform => ({platform, url:$(`social-${platform}`).value.trim()}))
    .filter(item => item.url);
}
function validateSocialLink({platform, url}) {
  if (!validateHttpsUrl(url)) return false;
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== 'https:' || !parsed.hostname.includes('.') || parsed.username || parsed.password ||
        (parsed.port && parsed.port !== '443')) return false;
    const allowed = SOCIAL_HOSTS[platform] || [];
    return !allowed.length || allowed.some(domain => parsed.hostname === domain || parsed.hostname.endsWith(`.${domain}`));
  } catch { return false; }
}
// Manually editable nine-color OYS palette. Values are validated before preview or send.
const THEME_KEYS = {
  primaryOrange: 'primary-orange', darkOrange: 'dark-orange', darkNavy: 'dark-navy',
  background: 'background', white: 'white', textPrimary: 'text-primary',
  textSecondary: 'text-secondary', borderColor: 'border-color', notificationRed: 'notification-red'
};
const THEME_DEFAULTS = Object.freeze({
  primaryOrange:'#F97316', darkOrange:'#EA580C', darkNavy:'#0D2A52',
  background:'#F9FAFB', white:'#FFFFFF', textPrimary:'#1F2937',
  textSecondary:'#4B5563', borderColor:'#E5E7EB', notificationRed:'#EF4444'
});
const validHex = value => /^#[A-Fa-f0-9]{6}$/.test(value);
function readTheme() {
  const values = {};
  for (const [key, name] of Object.entries(THEME_KEYS)) {
    const value = $(`theme-${key}`).value.trim();
    if (!validHex(value)) throw new Error(`Enter a valid 6-digit HEX value for --${name}, such as #F97316.`);
    values[key] = value.toUpperCase();
  }
  return values;
}
function setThemeColor(key, raw) {
  if (!(key in THEME_KEYS)) return;
  const input = $(`theme-${key}`);
  input.value = raw;
  const value = raw.trim();
  const valid = validHex(value);
  input.classList.toggle('invalid', !valid);
  input.setAttribute('aria-invalid', String(!valid));
  if (!valid) return; // Never inject partial or malformed CSS into preview.
  const normalized = value.toUpperCase();
  state.theme[key] = normalized;
  $(`theme-picker-${key}`).value = normalized;
  document.documentElement.style.setProperty(`--${THEME_KEYS[key]}`, normalized);
  preview();
}
function resetTheme() {
  for (const [key, value] of Object.entries(THEME_DEFAULTS)) setThemeColor(key, value);
}
function currentCss() {
  const colors = readTheme();
  return `:root {\n${Object.entries(THEME_KEYS).map(([key, name]) => `  --${name}: ${colors[key]};`).join('\n')}\n}`;
}
function applyCssPalette(source) {
  const withoutComments = source.replace(/\/\*[\s\S]*?\*\//g, '');
  const matches = [...withoutComments.matchAll(/--([a-z-]+)\s*:\s*([^;}\r\n]+)/g)];
  const palette = {};
  const byCssName = Object.fromEntries(Object.entries(THEME_KEYS).map(([key, css]) => [css, key]));
  for (const [, cssName, raw] of matches) {
    const key = byCssName[cssName];
    if (!key || palette[key]) throw new Error(`Unknown or repeated CSS variable: --${cssName}.`);
    const value = raw.trim();
    if (!validHex(value)) throw new Error(`Invalid color for --${cssName}. Use #RRGGBB.`);
    palette[key] = value.toUpperCase();
  }
  if (Object.keys(palette).length !== 9) throw new Error('The pasted palette must contain all nine OYS CSS variables.');
  for (const [key, value] of Object.entries(palette)) setThemeColor(key, value);
}
const MAX_REQUEST_BYTES = 35 * 1024 * 1024;
const REQUEST_OVERHEAD_BYTES = 256 * 1024; // leave room for form fields and multipart boundaries
function uploadTotal(extra = 0) {
  return (state.heroMode === 'custom' ? state.heroFile?.size || 0 : 0)
    + state.blocks.filter(b => b.type === 'image').reduce((sum, b) => sum + b.file.size, 0)
    + state.attachments.reduce((sum, f) => sum + f.size, 0) + extra;
}
function fitsUploadLimit(extra = 0) {
  return uploadTotal(extra) + REQUEST_OVERHEAD_BYTES <= MAX_REQUEST_BYTES;
}
const defaults = () => [
  {id: nextId(), type:'text', heading:'Hello Jetson,', text:'Welcome to Operation Yuva Sankalp!\n\nWe are pleased to share useful learning resources to support your academic and career journey.'},
  {id: nextId(), type:'text', heading:'Learning resources for you', text:'Explore the study materials, strengthen your fundamentals and practise with assessments designed to help you prepare with confidence.\n\nPlease find the relevant material attached to this email.'},
  {id: nextId(), type:'button', label:'Explore Learning Resources', url:'https://example.com/replace-with-your-course-link'},
  {id: nextId(), type:'text', heading:'', text:'Best regards,\nTeam Operation Yuva Sankalp\nBrainserve'}
];
let nextNumber = 0;
function nextId(){ return `block-${++nextNumber}`; }
const state = { blocks:[], heroMode:'default', heroFile:null, heroUrl:null, theme:{...THEME_DEFAULTS}, attachments:[], csrf:null, sending:false, dragId:null, device:'desktop' };
state.blocks = defaults();
let toastTimer;
function toast(message, type='info') {
  const node=$('toast'); node.textContent=message; node.className='toast show'+(type==='info'?'':` ${type}`);
  clearTimeout(toastTimer); toastTimer=setTimeout(()=>{node.classList.remove('show');},6000);
}
function makeObjectUrl(file) { return URL.createObjectURL(file); }
function releaseFileUrl(block) { if(block.urlObject) URL.revokeObjectURL(block.urlObject); }
function setHero(mode, file=null){
  if(state.heroUrl) URL.revokeObjectURL(state.heroUrl);
  state.heroMode=mode; state.heroFile=file; state.heroUrl=file?makeObjectUrl(file):null;
  $('hideHero').checked=mode==='none';
  $('heroThumbnail').src=state.heroUrl || '/images/oys-banner.png';
  $('heroThumbnail').classList.toggle('hidden-hero',mode==='none');
  $('bannerState').textContent=mode==='custom'?'Custom image':mode==='none'?'Hidden':'Original';
  $('bannerState').style.background=mode==='none'?'#f0f1f3':'';
  preview();
}
function renderBlocks(){
  $('blockList').innerHTML=state.blocks.map((b,i)=>{
    const names={text:'Text',notice:'Notice',image:'Image',button:'Button',divider:'Divider'};
    const symbols={text:'T',notice:'!',image:'▧',button:'↗',divider:'—'};
    let content='';
    if(b.type==='text'||b.type==='notice') content=`<div class="block-field"><label for="head-${b.id}">Heading (optional)</label><input id="head-${b.id}" data-field="heading" maxlength="150" value="${esc(b.heading)}" placeholder="Section title or greeting"></div><div class="block-field"><label for="text-${b.id}">Content</label><textarea id="text-${b.id}" data-field="text" placeholder="Write your message here…">${esc(b.text)}</textarea></div>`;
    if(b.type==='image') content=`<img class="image-block-preview" src="${esc(b.urlObject)}" alt="Uploaded email image preview"><div class="image-block-details"><small>${esc(b.file.name)} · ${formatSize(b.file.size)}</small><button class="outline-btn" data-action="replace" type="button">Replace</button></div>`;
    if(b.type==='button') content=`<div class="block-field"><label>Button label</label><input data-field="label" maxlength="90" value="${esc(b.label)}" placeholder="See learning resources"></div><div class="block-field"><label>Secure destination URL (https://)</label><input data-field="url" value="${esc(b.url)}" placeholder="https://example.org/course"></div>`;
    if(b.type==='divider') content='<span style="color:#939aa3;font-size:11px">Subtle horizontal separator between sections</span>';
    return `<div class="block-card" data-id="${b.id}" draggable="true"><div class="block-card-top"><span class="block-title"><span>${symbols[b.type]}</span> ${names[b.type]} ${i+1}</span><div class="block-actions"><button data-action="up" title="Move up" aria-label="Move block up" ${i===0?'disabled':''}>↑</button><button data-action="down" title="Move down" aria-label="Move block down" ${i===state.blocks.length-1?'disabled':''}>↓</button><button data-action="remove" title="Remove block" aria-label="Remove block">×</button></div></div><div class="block-body">${content}</div></div>`;
  }).join('');
  preview();
}
function moveBlock(id, step){
  const index=state.blocks.findIndex(b=>b.id===id); const next=index+step;
  if(index<0||next<0||next>=state.blocks.length) return;
  [state.blocks[index],state.blocks[next]]=[state.blocks[next],state.blocks[index]];
  renderBlocks();
}
function chooseImage(callback){
  const input=document.createElement('input'); input.type='file'; input.accept='image/png,image/jpeg';
  input.onchange=()=>{const file=input.files?.[0]; if(!file) return; if(!['image/png','image/jpeg'].includes(file.type)||file.size>5*1024*1024) return toast('Use a PNG or JPG image of 5 MB or less.','error'); callback(file);};
  input.click();
}
function addBlock(type){
  if(state.blocks.length>=25) return toast('A message can contain up to 25 content blocks.','error');
  if(type==='image'){
    const images=state.blocks.filter(b=>b.type==='image').length;
    if(images>=8) return toast('You can add up to 8 image blocks.','error');
    chooseImage(file=>{if(!fitsUploadLimit(file.size))return toast('Combined images and attachments exceed the 35 MB request limit.','error');state.blocks.push({id:nextId(),type:'image',file,urlObject:makeObjectUrl(file)});renderBlocks();}); return;
  }
  const template={text:{heading:'',text:'Write your message here…'},notice:{heading:'Important update',text:'Add your important message here…'},button:{label:'View resources',url:'https://example.com/replace-this-link'},divider:{}}[type];
  if(template){state.blocks.push({id:nextId(),type,...template});renderBlocks();}
}
function addAttachments(files){
  for(const file of files){
    if(state.attachments.length>=6){toast('Maximum 6 attachments per message.','error');break;}
    if(file.size===0){toast(`Empty file skipped: ${file.name}`,'error');continue;}
    if(file.size>10*1024*1024){toast(`File exceeds 10 MB: ${file.name}`,'error');continue;}
    if(!fitsUploadLimit(file.size)){toast('Combined files exceed the 35 MB request limit. Remove an existing file first.','error');continue;}
    if(state.attachments.some(f=>f.name===file.name&&f.size===file.size)) continue;
    state.attachments.push(file);
  }
  renderAttachments();
}
function renderAttachments(){
  $('attachmentList').innerHTML=state.attachments.map((file,i)=>`<div class="attachment"><div><strong>📎 ${esc(file.name)}</strong><small>${formatSize(file.size)}</small></div><button type="button" data-remove-file="${i}" aria-label="Remove attachment">×</button></div>`).join('');
}
function emailBodyHTML(){
  const t=state.theme;
  const logo=`<tr><td align="left" style="padding:17px 28px;background:${t.white};border-bottom:1px solid ${t.borderColor}"><img src="/images/brainserve-logo.png" width="228" alt="Brainserve" style="display:block;width:228px;max-width:100%;height:auto;border:0"></td></tr>`;
  const hero=state.heroMode==='none'?'':`<tr><td style="padding:0"><img width="640" src="${state.heroUrl||'/images/oys-banner.png'}" alt="Operation Yuva Sankalp banner" style="display:block;width:100%;max-width:640px;height:auto"></td></tr>`;
  let body='';
  for(const b of state.blocks){
    if(b.type==='text') body+=`<tr><td style="padding:25px 36px 12px;line-height:1.7;font-size:16px;color:${t.textPrimary}">${b.heading?.trim()?`<h2 style="font-size:22px;color:${t.darkNavy};line-height:1.35;margin:0 0 12px">${esc(b.heading)}</h2>`:''}<div style="margin:0 0 12px">${esc(b.text).replace(/\r?\n/g,'<br>')}</div></td></tr>`;
    if(b.type==='notice') body+=`<tr><td style="padding:10px 36px 20px"><table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:${t.background};border-left:4px solid ${t.notificationRed}"><tr><td style="padding:15px 17px;color:${t.textPrimary};font-size:15px;line-height:1.6">${b.heading?.trim()?`<strong style="display:block;margin-bottom:6px;color:${t.notificationRed}">${esc(b.heading)}</strong>`:''}${esc(b.text).replace(/\r?\n/g,'<br>')}</td></tr></table></td></tr>`;
    if(b.type==='image') body+=`<tr><td style="padding:0"><img width="640" alt="Email content image" src="${b.urlObject}" style="display:block;width:100%;max-width:640px;height:auto"></td></tr>`;
    if(b.type==='button') body+=`<tr><td style="padding:12px 36px 25px"><span style="display:inline-block;background:${t.primaryOrange};color:${t.white};padding:13px 22px;border-radius:7px;font-weight:bold;font-size:15px">${esc(b.label)}</span></td></tr>`;
    if(b.type==='divider') body+=`<tr><td style="padding:14px 36px"><div style="border-top:1px solid ${t.borderColor};font-size:0;line-height:0">&nbsp;</div></td></tr>`;
  }
  const links=socialLinks().filter(validateSocialLink); // Only valid HTTPS destinations open from the preview.
  const social=!links.length?'':`<tr><td align="center" style="padding:23px 24px 27px;background:${t.background};border-top:1px solid ${t.borderColor}"><p style="margin:0 0 16px;color:${t.darkNavy};font-size:16px;font-weight:bold;line-height:1.5">${esc($('socialTitle').value.trim() || 'Connect with Brainserve')}</p>${links.map(link => `<a href="${esc(link.url)}" target="_blank" rel="noopener noreferrer" style="display:inline-block;vertical-align:top;text-align:center;width:77px;margin:0 3px 9px;padding:5px 2px;color:${t.darkOrange};text-decoration:none;font-size:11px;font-weight:bold;line-height:1.35"><img src="/images/social/${link.platform}.png" alt="${SOCIAL_LABELS[link.platform]}" width="36" height="36" style="display:block;width:36px;height:36px;margin:0 auto 6px;border:0"><span style="display:block">${SOCIAL_LABELS[link.platform]}</span></a>`).join('')}</td></tr>`;
  return `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1" /></head><body style="padding:0;margin:0;background:${t.background};font:14px Arial,Helvetica,sans-serif;color:${t.textPrimary}"><table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background:${t.background}"><tr><td align="center" style="padding:15px 0"><table role="presentation" cellpadding="0" cellspacing="0" border="0" width="640" style="width:100%;max-width:640px;background:${t.white}">${logo}${hero}${body}${social}<tr><td style="padding:23px 36px 28px;border-top:1px solid ${t.borderColor}"><p style="margin:0;color:${t.textSecondary};font-size:12px;line-height:1.6">Operation Yuva Sankalp · Student Learning Initiative</p></td></tr></table></td></tr></table></body></html>`;
}
function preview(){
  $('previewSubject').textContent=$('subject').value.trim()||'(No subject)';
  $('previewSender').textContent=$('fromName').value.trim()||'Operation Yuva Sankalp';
  $('previewAddress').textContent='to: '+($('to').value.trim().split(/[,;\n]/)[0]||'recipient@example.com');
  const iframe=$('previewFrame'); iframe.srcdoc=emailBodyHTML();
}
function resizeFrame(){
  const iframe=$('previewFrame');
  try {const doc=iframe.contentDocument; if(!doc) return; const measure=()=>{const h=Math.max(doc.documentElement.scrollHeight,doc.body?.scrollHeight||0);iframe.style.height=Math.max(270,h)+'px';};measure();doc.querySelectorAll('img').forEach(img=>img.addEventListener('load',measure,{once:true}));}
  catch(_e){iframe.style.height='660px';}
}
function parseTo(){return $('to').value.split(/[,;\n]/).map(t=>t.trim()).filter(Boolean);}
function validate(){
  const recipients=parseTo();
  if(!recipients.length) throw new Error('Enter at least one recipient.');
  if(recipients.length>50) throw new Error('You can send to at most 50 recipients.');
  if(!recipients.every(v=>/^[^\s@,;]+@[^\s@,;]+\.[^\s@,;]+$/.test(v))) throw new Error('Enter a valid recipient email address.');
  if(!$('from').value) throw new Error('Select an authorized From address.');
  if(!$('subject').value.trim()) throw new Error('Add an email subject.');
  if(!$('fromName').value.trim()) throw new Error('Enter a sender name.');
  if($('replyTo').value.trim()&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test($('replyTo').value.trim())) throw new Error('Reply-to address is invalid.');
  if(!state.blocks.length) throw new Error('Add at least one body section.');
  if(!fitsUploadLimit()) throw new Error('Combined files exceed the 35 MB upload limit. Remove or reduce files.');
  readTheme();
  for(const b of state.blocks){
    if((b.type==='text'||b.type==='notice')&&!((b.heading||'').trim()||(b.text||'').trim())) throw new Error('Text and notice blocks cannot be empty.');
    if(b.type==='button') {
      if(!(b.label||'').trim() || !validateHttpsUrl(b.url)) throw new Error('Each button needs a label and a valid HTTPS URL.');
    }
    if(b.type==='button'&&b.url.includes('example.com/replace-')) throw new Error('Replace the example button link with your actual resource URL before sending.');
  }
  if ($('socialEnabled').checked) {
    const title = $('socialTitle').value.trim();
    if (title.length > 100 || /[\r\n]/.test(title)) throw new Error('Social footer heading must be one line (up to 100 characters).');
    for (const link of socialLinks()) {
      if (!validateSocialLink(link)) throw new Error(`Enter a valid HTTPS link to your ${SOCIAL_LABELS[link.platform]} page (no shortened or unrelated domains).`);
    }
  }
  return recipients;
}
function serializeBlocks(){let imageIndex=0;return state.blocks.map(b=>({type:b.type,heading:b.heading||null,text:b.text||null,imageIndex:b.type==='image'?imageIndex++:null,label:b.label||null,url:b.type==='button'?b.url||null:null}));}
function openConfirmation(){
  try{const recipients=validate();$('modalText').textContent=`Ready to send “${$('subject').value.trim()}” to ${recipients.length} recipient${recipients.length===1?'':'s'}? This will use your configured SMTP server.`;$('modal').hidden=false;}
  catch(e){toast(e.message,'error');}
}
async function sendNow(){
  if(state.sending)return;
  state.sending=true;
  $('confirmSend').disabled=true;$('sendBtn').disabled=true;$('sendBottom').disabled=true;
  $('confirmSend').textContent='Sending…';
  try{
    validate();
    const data=new FormData();
    for(const name of ['to','from','fromName','replyTo','subject'])data.append(name,$(name).value.trim());
    data.append('heroMode',state.heroMode);data.append('accentColor',state.theme.primaryOrange);data.append('themeJson',JSON.stringify(readTheme()));data.append('blocksJson',JSON.stringify(serializeBlocks()));
    data.append('socialTitle',$('socialTitle').value.trim());data.append('socialLinksJson',JSON.stringify(socialLinks()));
    if(state.heroMode==='custom')data.append('heroBanner',state.heroFile,state.heroFile.name);
    state.blocks.filter(b=>b.type==='image').forEach(b=>data.append('inlineImages',b.file,b.file.name));
    state.attachments.forEach(file=>data.append('attachments',file,file.name));
    if (!state.csrf?.header || !state.csrf?.token) throw new Error('Email configuration is not ready. Refresh the editor.');
    const response=await fetch('/api/send',{method:'POST',body:data,headers:{[state.csrf.header]:state.csrf.token}});
    const result=await response.json().catch(()=>({}));
    if(!response.ok) {
      if(response.status===401||response.status===403) throw new Error('Security token expired or invalid. Refresh the editor and try again.');
      throw new Error(result.error||`Could not send email (HTTP ${response.status}).`);
    }
    $('modal').hidden=true;
    toast(`Message accepted by SMTP for ${result.recipientCount} recipient${result.recipientCount===1?'':'s'}.`,'success');
  }catch(e){toast(e.message||'Email could not be sent.','error');}
  finally{state.sending=false;$('confirmSend').disabled=false;$('sendBtn').disabled=false;$('sendBottom').disabled=false;$('confirmSend').textContent='Confirm & send';}
}
function resetDraft(){
  if(!window.confirm('Reset the design? Your current text, banners, and attachments on this page will be discarded.'))return;
  state.blocks.forEach(releaseFileUrl);state.blocks=defaults();state.attachments=[];
  setHero('default');$('subject').value='Your learning journey starts here | Operation Yuva Sankalp';
  $('fromName').value='Operation Yuva Sankalp';$('to').value='';$('replyTo').value='';resetTheme();
  $('socialEnabled').checked=true;$('socialFields').hidden=false;$('socialTitle').value='Connect with Brainserve';
  SOCIAL_PLATFORMS.forEach(platform=>$(`social-${platform}`).value='');
  renderBlocks();renderAttachments();toast('Design reset.');
}
function installEvents(){
  ['subject','to','fromName','replyTo'].forEach(id=>$(id).addEventListener('input',preview));
  document.querySelectorAll('[data-theme-key]').forEach(input=>input.addEventListener('input',e=>setThemeColor(e.target.dataset.themeKey,e.target.value)));
  document.querySelectorAll('[data-theme-pick]').forEach(input=>input.addEventListener('input',e=>setThemeColor(e.target.dataset.themePick,e.target.value)));
  $('resetTheme').addEventListener('click',()=>{resetTheme();toast('OYS brand colors restored.');});
  $('applyThemeCss').addEventListener('click',()=>{try{applyCssPalette($('themeCssInput').value);toast('All nine brand colors applied.','success');}catch(e){toast(e.message,'error');}});
  $('copyThemeCss').addEventListener('click',async()=>{try{const css=currentCss();$('themeCssInput').value=css;if(navigator.clipboard?.writeText){await navigator.clipboard.writeText(css);toast('CSS variables copied.','success');}else{ $('themeCssInput').focus();$('themeCssInput').select();toast('CSS ready to copy from the text box.'); }}catch(e){toast(e.message,'error');}});
  $('socialEnabled').addEventListener('change',e=>{$('socialFields').hidden=!e.target.checked;preview();});
  $('socialTitle').addEventListener('input',preview);
  document.querySelectorAll('[data-social-platform]').forEach(input=>input.addEventListener('input',preview));
  $('changeHero').addEventListener('click',()=>$('heroFile').click());
  $('heroFile').addEventListener('change',e=>{const file=e.target.files[0];if(!file)return;if(!['image/png','image/jpeg'].includes(file.type)||file.size>5*1024*1024)return toast('Use a PNG or JPG banner of up to 5 MB.','error');const previous=state.heroMode==='custom'?(state.heroFile?.size||0):0;if(!fitsUploadLimit(file.size-previous))return toast('Combined files exceed the 35 MB upload limit.','error');setHero('custom',file);e.target.value='';});
  $('resetHero').addEventListener('click',()=>setHero('default'));
  $('hideHero').addEventListener('change',e=>{if(e.target.checked){setHero('none');}else{setHero('default');}});
  document.querySelectorAll('[data-add]').forEach(b=>b.addEventListener('click',()=>addBlock(b.dataset.add)));
  $('blockList').addEventListener('input',e=>{
    const field=e.target.dataset.field,id=e.target.closest('[data-id]')?.dataset.id;if(!field||!id)return;
    const block=state.blocks.find(b=>b.id===id);if(!block)return;block[field]=e.target.value;preview();
  });
  $('blockList').addEventListener('click',e=>{
    const button=e.target.closest('[data-action]');if(!button)return;
    const id=button.closest('[data-id]')?.dataset.id;
    if(button.dataset.action==='up')moveBlock(id,-1);
    if(button.dataset.action==='down')moveBlock(id,1);
    if(button.dataset.action==='remove'){const index=state.blocks.findIndex(b=>b.id===id);if(index>=0){releaseFileUrl(state.blocks[index]);state.blocks.splice(index,1);renderBlocks();}}
    if(button.dataset.action==='replace')chooseImage(file=>{const block=state.blocks.find(b=>b.id===id);if(block){if(!fitsUploadLimit(file.size-block.file.size))return toast('Combined files exceed the 35 MB upload limit.','error');releaseFileUrl(block);block.file=file;block.urlObject=makeObjectUrl(file);renderBlocks();}});
  });
  $('blockList').addEventListener('dragstart',e=>{const card=e.target.closest('[data-id]');if(!card)return;state.dragId=card.dataset.id;e.dataTransfer.effectAllowed='move';});
  $('blockList').addEventListener('dragover',e=>{if(e.target.closest('[data-id]'))e.preventDefault();});
  $('blockList').addEventListener('drop',e=>{e.preventDefault();const target=e.target.closest('[data-id]')?.dataset.id;if(!target||!state.dragId||target===state.dragId)return;const from=state.blocks.findIndex(b=>b.id===state.dragId),to=state.blocks.findIndex(b=>b.id===target);if(from>=0&&to>=0){state.blocks.splice(to,0,state.blocks.splice(from,1)[0]);renderBlocks();}state.dragId=null;});
  const upload=()=> $('attachmentFiles').click();
  $('dropZone').addEventListener('click',upload);
  $('dropZone').addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();upload();}});
  $('attachmentFiles').addEventListener('change',e=>{addAttachments([...e.target.files]);e.target.value='';});
  $('dropZone').addEventListener('dragover',e=>{e.preventDefault();$('dropZone').classList.add('drag-over');});
  $('dropZone').addEventListener('dragleave',()=>$('dropZone').classList.remove('drag-over'));
  $('dropZone').addEventListener('drop',e=>{e.preventDefault();$('dropZone').classList.remove('drag-over');addAttachments([...e.dataTransfer.files]);});
  $('attachmentList').addEventListener('click',e=>{const b=e.target.closest('[data-remove-file]');if(!b)return;state.attachments.splice(Number(b.dataset.removeFile),1);renderAttachments();});
  $('desktopView').addEventListener('click',()=>setDevice('desktop'));
  $('mobileView').addEventListener('click',()=>setDevice('mobile'));
  $('previewFrame').addEventListener('load',resizeFrame);
  ['sendBtn','sendBottom'].forEach(id=>$(id).addEventListener('click',openConfirmation));
  $('cancelSend').addEventListener('click',()=>$('modal').hidden=true);
  $('modal').addEventListener('click',e=>{if(e.target===$('modal'))$('modal').hidden=true;});
  $('confirmSend').addEventListener('click',sendNow);
  $('resetDraft').addEventListener('click',resetDraft);
}
function setDevice(device){state.device=device;$('previewCanvas').classList.toggle('mobile',device==='mobile');$('desktopView').classList.toggle('selected',device==='desktop');$('mobileView').classList.toggle('selected',device==='mobile');}
async function init(){
  installEvents();renderBlocks();
  try{
    const response=await fetch('/api/config',{credentials:'same-origin'});
    if(!response.ok)throw new Error('Cannot load email configuration. Check that the server is running and refresh this page.');
    const config=await response.json();
    const options=Array.from(new Set(config.allowedFrom||[config.defaultFrom]));
    $('from').replaceChildren(...options.map(address=>new Option(address,address)));
    $('from').value=config.defaultFrom;
    state.csrf={header:config.csrfHeader,token:config.csrfToken};
  }catch(e){toast(e.message,'error');['sendBtn','sendBottom'].forEach(id=>$(id).disabled=true);}
  preview();
}
init();
