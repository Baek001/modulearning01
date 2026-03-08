import { Buffer } from "node:buffer";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  CallableAuth,
  JsonMap,
  asObject,
  assertSignedIn,
  db,
  ensureBaselineData,
  nowIso,
  toIso,
  trimmed,
} from "./shared.js";
import { listDirectoryUsersData, loadProfile } from "./profile.js";

const CONTRACT_BOOTSTRAP = "system/contractBootstrap";
const CONTRACTS = "contracts";
const CONTRACT_BATCHES = "contractBatches";
const CONTRACT_TEMPLATES = "contractTemplates";
const CONTRACT_REQUESTS = "contractTemplateRequests";
const CONTRACT_PUBLIC = "contractPublicTokens";
const CONTRACT_SETTINGS = "contractSettings";

type StoredFile = {
  name: string;
  path: string;
  downloadURL: string;
  size: number;
  contentType?: string;
};

type TemplateVersion = {
  templateVersionId: string;
  versionNo: number;
  versionLabel: string;
  versionNote: string;
  schema: Record<string, unknown>;
  layout: Record<string, unknown>;
  sourceFile?: StoredFile | null;
  backgroundFile?: StoredFile | null;
  createdAt: string;
  createdBy: string;
};

type ContractTemplate = {
  templateId: string;
  templateNm: string;
  templateCode: string;
  templateDesc: string;
  templateCategoryCd: string;
  currentVersionId: string;
  publishedVersionId: string;
  versions: TemplateVersion[];
  createdAt: string;
  updatedAt: string;
};

type TemplateRequest = {
  requestId: string;
  requestTitle: string;
  requestNote: string;
  statusCd: string;
  requesterUserId: string;
  requesterUserNm: string;
  sourceFile?: StoredFile | null;
  markedFile?: StoredFile | null;
  sealFile?: StoredFile | null;
  crtDt: string;
  lastChgDt: string;
};

type ContractSigner = {
  signerId: string;
  signerOrder: number;
  signerNm: string;
  signerEmail: string;
  signerTelno: string;
  claimedNm: string;
  claimedEmail: string;
  claimedTelno: string;
  statusCd: string;
  submittedDt: string;
  token: string;
  signatureData?: string;
  initialData?: string;
};

type ContractRecord = {
  contractId: string;
  contractRef: string;
  templateId: string;
  templateNm: string;
  contractTitle: string;
  contractMessage: string;
  sendTypeCd: string;
  signingFlowCd: string;
  statusCd: string;
  expiresDt: string;
  sentDt: string;
  lastChgDt: string;
  crtDt: string;
  creatorUserId: string;
  creatorUserNm: string;
  currentSignOrder: number;
  totalSignerCount: number;
  signedCount: number;
  signers: ContractSigner[];
  fieldMap: Record<string, unknown>;
  templateSchema: Record<string, unknown>;
  templateLayout: Record<string, unknown>;
  cancelReason?: string;
};

type CompanySettings = {
  companyNm: string;
  senderNm: string;
  senderEmail: string;
  senderTelno: string;
  sealFile?: StoredFile | null;
  updatedAt: string;
};

function makeId(prefix: string) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function normalizeStoredFile(item: unknown): StoredFile | null {
  const source = asObject(item);
  const path = trimmed(source.path) || trimmed(source.filePath) || trimmed(source.downloadURL);
  if (!path) {
    return null;
  }
  return {
    name: trimmed(source.name) || trimmed(source.orgnFileNm) || "file",
    path,
    downloadURL: trimmed(source.downloadURL) || trimmed(source.filePath) || path,
    size: Number(source.size || source.fileSize || 0) || 0,
    contentType: trimmed(source.contentType),
  };
}

function normalizeVersion(item: unknown): TemplateVersion {
  const source = asObject(item);
  return {
    templateVersionId: trimmed(source.templateVersionId) || makeId("TMPL_VER"),
    versionNo: Number(source.versionNo || 1) || 1,
    versionLabel: trimmed(source.versionLabel) || "v1.0",
    versionNote: trimmed(source.versionNote),
    schema: asObject(source.schema),
    layout: asObject(source.layout),
    sourceFile: normalizeStoredFile(source.sourceFile),
    backgroundFile: normalizeStoredFile(source.backgroundFile),
    createdAt: toIso(source.createdAt) || nowIso(),
    createdBy: trimmed(source.createdBy),
  };
}

function normalizeTemplate(item: unknown): ContractTemplate {
  const source = asObject(item);
  const versions = Array.isArray(source.versions)
    ? source.versions.map((entry) => normalizeVersion(entry))
    : [];
  return {
    templateId: trimmed(source.templateId),
    templateNm: trimmed(source.templateNm) || "이름 없는 템플릿",
    templateCode: trimmed(source.templateCode),
    templateDesc: trimmed(source.templateDesc),
    templateCategoryCd: trimmed(source.templateCategoryCd) || "general",
    currentVersionId: trimmed(source.currentVersionId) || versions[0]?.templateVersionId || "",
    publishedVersionId: trimmed(source.publishedVersionId),
    versions,
    createdAt: toIso(source.createdAt) || nowIso(),
    updatedAt: toIso(source.updatedAt) || nowIso(),
  };
}

function normalizeSigner(item: unknown, index = 0): ContractSigner {
  const source = asObject(item);
  return {
    signerId: trimmed(source.signerId) || makeId(`SIGNER_${index + 1}`),
    signerOrder: Number(source.signerOrder || index + 1) || index + 1,
    signerNm: trimmed(source.signerNm),
    signerEmail: trimmed(source.signerEmail),
    signerTelno: trimmed(source.signerTelno),
    claimedNm: trimmed(source.claimedNm) || trimmed(source.signerNm),
    claimedEmail: trimmed(source.claimedEmail) || trimmed(source.signerEmail),
    claimedTelno: trimmed(source.claimedTelno) || trimmed(source.signerTelno),
    statusCd: trimmed(source.statusCd) || "pending",
    submittedDt: toIso(source.submittedDt),
    token: trimmed(source.token) || makeId("PUBLIC"),
    signatureData: trimmed(source.signatureData),
    initialData: trimmed(source.initialData),
  };
}

function normalizeContract(item: unknown): ContractRecord {
  const source = asObject(item);
  const signers = Array.isArray(source.signers)
    ? source.signers.map((entry, index) => normalizeSigner(entry, index))
    : [];
  return {
    contractId: trimmed(source.contractId),
    contractRef: trimmed(source.contractRef),
    templateId: trimmed(source.templateId),
    templateNm: trimmed(source.templateNm),
    contractTitle: trimmed(source.contractTitle),
    contractMessage: trimmed(source.contractMessage),
    sendTypeCd: trimmed(source.sendTypeCd) || "remote",
    signingFlowCd: trimmed(source.signingFlowCd) || "parallel",
    statusCd: trimmed(source.statusCd) || "draft",
    expiresDt: toIso(source.expiresDt),
    sentDt: toIso(source.sentDt),
    lastChgDt: toIso(source.lastChgDt) || nowIso(),
    crtDt: toIso(source.crtDt) || nowIso(),
    creatorUserId: trimmed(source.creatorUserId),
    creatorUserNm: trimmed(source.creatorUserNm),
    currentSignOrder: Number(source.currentSignOrder || 1) || 1,
    totalSignerCount: Number(source.totalSignerCount || signers.length) || signers.length,
    signedCount: Number(source.signedCount || signers.filter((signer) => signer.statusCd === "completed").length) || 0,
    signers,
    fieldMap: asObject(source.fieldMap),
    templateSchema: asObject(source.templateSchema),
    templateLayout: asObject(source.templateLayout),
    cancelReason: trimmed(source.cancelReason),
  };
}

function normalizeTemplateRequest(item: unknown): TemplateRequest {
  const source = asObject(item);
  return {
    requestId: trimmed(source.requestId),
    requestTitle: trimmed(source.requestTitle),
    requestNote: trimmed(source.requestNote),
    statusCd: trimmed(source.statusCd) || "pending",
    requesterUserId: trimmed(source.requesterUserId),
    requesterUserNm: trimmed(source.requesterUserNm),
    sourceFile: normalizeStoredFile(source.sourceFile),
    markedFile: normalizeStoredFile(source.markedFile),
    sealFile: normalizeStoredFile(source.sealFile),
    crtDt: toIso(source.crtDt) || nowIso(),
    lastChgDt: toIso(source.lastChgDt) || nowIso(),
  };
}

function normalizeCompanySettings(item: unknown): CompanySettings {
  const source = asObject(item);
  return {
    companyNm: trimmed(source.companyNm) || "StarWorks",
    senderNm: trimmed(source.senderNm) || "전자계약 관리자",
    senderEmail: trimmed(source.senderEmail) || "contracts@starworks.local",
    senderTelno: trimmed(source.senderTelno) || "02-0000-0000",
    sealFile: normalizeStoredFile(source.sealFile),
    updatedAt: toIso(source.updatedAt) || nowIso(),
  };
}

function normalizeAttachmentsPayload(data: JsonMap) {
  return {
    sourceFile: normalizeStoredFile(data.sourceFile),
    backgroundFile: normalizeStoredFile(data.backgroundFile),
    markedFile: normalizeStoredFile(data.markedFile),
    sealFile: normalizeStoredFile(data.sealFile),
  };
}

function summarizeContract(contract: ContractRecord) {
  return {
    contractId: contract.contractId,
    contractRef: contract.contractRef,
    contractTitle: contract.contractTitle,
    contractMessage: contract.contractMessage,
    templateId: contract.templateId,
    templateNm: contract.templateNm,
    statusCd: contract.statusCd,
    sendTypeCd: contract.sendTypeCd,
    signingFlowCd: contract.signingFlowCd,
    expiresDt: contract.expiresDt,
    sentDt: contract.sentDt,
    lastChgDt: contract.lastChgDt,
    crtDt: contract.crtDt,
    totalSignerCount: contract.totalSignerCount,
    signedCount: contract.signedCount,
  };
}

function summarizeTemplate(template: ContractTemplate) {
  return {
    templateId: template.templateId,
    templateNm: template.templateNm,
    templateCode: template.templateCode,
    templateDesc: template.templateDesc,
    templateCategoryCd: template.templateCategoryCd,
    currentVersion: template.versions.find((version) => version.templateVersionId === template.currentVersionId) || template.versions[0] || null,
    versions: template.versions,
    lastChgDt: template.updatedAt,
    crtDt: template.createdAt,
  };
}

function summarizeTemplateRequest(requestItem: TemplateRequest) {
  return {
    requestId: requestItem.requestId,
    requestTitle: requestItem.requestTitle,
    requestNote: requestItem.requestNote,
    statusCd: requestItem.statusCd,
    crtDt: requestItem.crtDt,
    lastChgDt: requestItem.lastChgDt,
  };
}

function ensureTemplateVersion(template: ContractTemplate, templateVersionId: string) {
  return template.versions.find((version) => version.templateVersionId === templateVersionId)
    || template.versions.find((version) => version.templateVersionId === template.currentVersionId)
    || template.versions[template.versions.length - 1];
}

function makeContractRef() {
  const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  return `CTR-${datePart}-${Math.random().toString().slice(2, 6)}`;
}

function decodeSection(section: string) {
  if (section === "completed") {
    return new Set(["completed"]);
  }
  if (section === "archive") {
    return new Set(["cancelled", "expired"]);
  }
  return new Set(["draft", "ready", "sent", "in_progress"]);
}

function escapePdfText(value: string) {
  return value.replace(/\\/g, "\\\\").replace(/\(/g, "\\(").replace(/\)/g, "\\)");
}

function buildSimplePdfBase64(lines: string[]) {
  const safeLines = lines.slice(0, 18);
  const content = safeLines
    .map((line, index) => `BT /F1 12 Tf 48 ${760 - (index * 24)} Td (${escapePdfText(line)}) Tj ET`)
    .join("\n");

  const objects: string[] = [];
  objects.push("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj");
  objects.push("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj");
  objects.push("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >> endobj");
  objects.push(`4 0 obj << /Length ${Buffer.byteLength(content, "utf8")} >> stream\n${content}\nendstream endobj`);
  objects.push("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj");

  let pdf = "%PDF-1.4\n";
  const offsets: number[] = [0];
  objects.forEach((object) => {
    offsets.push(Buffer.byteLength(pdf, "utf8"));
    pdf += `${object}\n`;
  });
  const xrefPosition = Buffer.byteLength(pdf, "utf8");
  pdf += `xref\n0 ${objects.length + 1}\n`;
  pdf += "0000000000 65535 f \n";
  offsets.slice(1).forEach((offset) => {
    pdf += `${String(offset).padStart(10, "0")} 00000 n \n`;
  });
  pdf += `trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefPosition}\n%%EOF`;
  return Buffer.from(pdf, "utf8").toString("base64");
}

function signerCanSubmit(contract: ContractRecord, signer: ContractSigner) {
  if (["cancelled", "expired", "completed"].includes(contract.statusCd)) {
    return { canSubmit: false, blockedReason: "현재 제출할 수 없는 계약입니다." };
  }
  if (contract.expiresDt && new Date(contract.expiresDt).getTime() < Date.now()) {
    return { canSubmit: false, blockedReason: "만료된 링크입니다." };
  }
  if (signer.statusCd === "completed") {
    return { canSubmit: false, blockedReason: "이미 서명을 완료했습니다." };
  }
  if (contract.signingFlowCd === "serial" && signer.signerOrder !== contract.currentSignOrder) {
    return { canSubmit: false, blockedReason: "현재 순서의 서명자가 아닙니다." };
  }
  return { canSubmit: true, blockedReason: "" };
}

function nextContractStatusAfterSubmit(contract: ContractRecord, signers: ContractSigner[]) {
  const signedCount = signers.filter((signer) => signer.statusCd === "completed").length;
  if (signedCount >= signers.length) {
    return {
      statusCd: "completed",
      currentSignOrder: contract.currentSignOrder,
      signedCount,
    };
  }
  if (contract.signingFlowCd === "serial") {
    const nextPending = signers
      .filter((signer) => signer.statusCd !== "completed")
      .sort((left, right) => left.signerOrder - right.signerOrder)[0];
    return {
      statusCd: "in_progress",
      currentSignOrder: nextPending?.signerOrder || contract.currentSignOrder,
      signedCount,
    };
  }
  return {
    statusCd: "in_progress",
    currentSignOrder: contract.currentSignOrder,
    signedCount,
  };
}

async function syncPublicTokens(contract: ContractRecord) {
  const batch = db.batch();
  contract.signers.forEach((signer) => {
    batch.set(db.collection(CONTRACT_PUBLIC).doc(signer.token), {
      contractId: contract.contractId,
      signerId: signer.signerId,
      token: signer.token,
      updatedAt: nowIso(),
    }, { merge: true });
  });
  await batch.commit();
}

async function ensureContractSeedData() {
  await ensureBaselineData();
  const bootstrapRef = db.doc(CONTRACT_BOOTSTRAP);
  const snapshot = await bootstrapRef.get();
  if (snapshot.exists) {
    return;
  }

  const users = await listDirectoryUsersData();
  const admin = users.find((item) => item.userId === "admin") || users[0];
  const memberOne = users.find((item) => item.userId === "user01") || users[0];
  const memberTwo = users.find((item) => item.userId === "user02") || users[1] || users[0];
  const seededAt = nowIso();
  const templateIdOne = String(Date.now()).slice(-6);
  const templateIdTwo = String(Date.now() + 1).slice(-6);

  const versionOne: TemplateVersion = {
    templateVersionId: makeId("TMPL_VER"),
    versionNo: 1,
    versionLabel: "v1.0",
    versionNote: "기본 NDA 양식",
    schema: {
      pages: [{ pageId: "page-1", label: "1" }],
      fields: [
        { fieldId: "field-name", fieldKey: "recipientName", label: "수신자명", fieldTypeCd: "text", assignmentCd: "signer", signerOrder: 1, x: 80, y: 180, width: 220, height: 40 },
        { fieldId: "field-email", fieldKey: "recipientEmail", label: "이메일", fieldTypeCd: "email", assignmentCd: "signer", signerOrder: 1, x: 80, y: 240, width: 260, height: 40 },
      ],
    },
    layout: { canvas: { width: 820, height: 1160, padding: 48, backgroundColor: "#ffffff" } },
    createdAt: seededAt,
    createdBy: admin?.userId || "admin",
  };
  const versionTwo: TemplateVersion = {
    templateVersionId: makeId("TMPL_VER"),
    versionNo: 1,
    versionLabel: "v1.0",
    versionNote: "용역 계약 기본 양식",
    schema: {
      pages: [{ pageId: "page-1", label: "1" }],
      fields: [
        { fieldId: "field-company", fieldKey: "companyName", label: "회사명", fieldTypeCd: "text", assignmentCd: "creator", signerOrder: 1, x: 90, y: 190, width: 220, height: 40 },
        { fieldId: "field-contact", fieldKey: "contactName", label: "담당자", fieldTypeCd: "text", assignmentCd: "signer", signerOrder: 1, x: 90, y: 250, width: 220, height: 40 },
      ],
    },
    layout: { canvas: { width: 820, height: 1160, padding: 48, backgroundColor: "#ffffff" } },
    createdAt: seededAt,
    createdBy: admin?.userId || "admin",
  };

  const templates: ContractTemplate[] = [
    {
      templateId: templateIdOne,
      templateNm: "기본 비밀유지계약",
      templateCode: "NDA_BASIC",
      templateDesc: "사내 기본 NDA 양식입니다.",
      templateCategoryCd: "general",
      currentVersionId: versionOne.templateVersionId,
      publishedVersionId: versionOne.templateVersionId,
      versions: [versionOne],
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      templateId: templateIdTwo,
      templateNm: "용역 계약서",
      templateCode: "SERVICE_STD",
      templateDesc: "외부 용역 발주용 표준 계약서입니다.",
      templateCategoryCd: "service",
      currentVersionId: versionTwo.templateVersionId,
      publishedVersionId: versionTwo.templateVersionId,
      versions: [versionTwo],
      createdAt: seededAt,
      updatedAt: seededAt,
    },
  ];

  const sampleContract: ContractRecord = {
    contractId: makeId("CTR"),
    contractRef: `CTR-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-001`,
    templateId: templateIdOne,
    templateNm: templates[0].templateNm,
    contractTitle: "협력사 NDA 체결",
    contractMessage: "비밀유지계약 전환 테스트용 샘플입니다.",
    sendTypeCd: "remote",
    signingFlowCd: "parallel",
    statusCd: "sent",
    expiresDt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 7).toISOString(),
    sentDt: seededAt,
    lastChgDt: seededAt,
    crtDt: seededAt,
    creatorUserId: admin?.userId || "admin",
    creatorUserNm: admin?.userNm || "관리자",
    currentSignOrder: 1,
    totalSignerCount: 1,
    signedCount: 0,
    signers: [
      {
        signerId: makeId("SIGNER"),
        signerOrder: 1,
        signerNm: memberOne?.userNm || "사용자",
        signerEmail: memberOne?.userEmail || "user01@starworks.local",
        signerTelno: memberOne?.userTelno || "010-0000-0001",
        claimedNm: memberOne?.userNm || "사용자",
        claimedEmail: memberOne?.userEmail || "user01@starworks.local",
        claimedTelno: memberOne?.userTelno || "010-0000-0001",
        statusCd: "pending",
        submittedDt: "",
        token: makeId("PUBLIC"),
      },
    ],
    fieldMap: { recipientName: memberOne?.userNm || "사용자", recipientEmail: memberOne?.userEmail || "user01@starworks.local" },
    templateSchema: versionOne.schema,
    templateLayout: versionOne.layout,
  };

  const completedContract: ContractRecord = {
    contractId: makeId("CTR"),
    contractRef: `CTR-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-002`,
    templateId: templateIdTwo,
    templateNm: templates[1].templateNm,
    contractTitle: "외주 개발 계약",
    contractMessage: "완료된 계약 샘플입니다.",
    sendTypeCd: "remote",
    signingFlowCd: "serial",
    statusCd: "completed",
    expiresDt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 2).toISOString(),
    sentDt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
    lastChgDt: new Date(Date.now() - 1000 * 60 * 60).toISOString(),
    crtDt: new Date(Date.now() - 1000 * 60 * 60 * 48).toISOString(),
    creatorUserId: admin?.userId || "admin",
    creatorUserNm: admin?.userNm || "관리자",
    currentSignOrder: 2,
    totalSignerCount: 2,
    signedCount: 2,
    signers: [
      {
        signerId: makeId("SIGNER"),
        signerOrder: 1,
        signerNm: memberOne?.userNm || "사용자",
        signerEmail: memberOne?.userEmail || "user01@starworks.local",
        signerTelno: memberOne?.userTelno || "010-0000-0001",
        claimedNm: memberOne?.userNm || "사용자",
        claimedEmail: memberOne?.userEmail || "user01@starworks.local",
        claimedTelno: memberOne?.userTelno || "010-0000-0001",
        statusCd: "completed",
        submittedDt: new Date(Date.now() - 1000 * 60 * 60 * 23).toISOString(),
        token: makeId("PUBLIC"),
      },
      {
        signerId: makeId("SIGNER"),
        signerOrder: 2,
        signerNm: memberTwo?.userNm || "사용자2",
        signerEmail: memberTwo?.userEmail || "user02@starworks.local",
        signerTelno: memberTwo?.userTelno || "010-0000-0002",
        claimedNm: memberTwo?.userNm || "사용자2",
        claimedEmail: memberTwo?.userEmail || "user02@starworks.local",
        claimedTelno: memberTwo?.userTelno || "010-0000-0002",
        statusCd: "completed",
        submittedDt: new Date(Date.now() - 1000 * 60 * 60 * 20).toISOString(),
        token: makeId("PUBLIC"),
      },
    ],
    fieldMap: { companyName: "StarWorks", contactName: memberTwo?.userNm || "사용자2" },
    templateSchema: versionTwo.schema,
    templateLayout: versionTwo.layout,
  };

  const requestItem: TemplateRequest = {
    requestId: makeId("REQ"),
    requestTitle: "인사 계약 템플릿 요청",
    requestNote: "연봉계약용 전용 서식이 필요합니다.",
    statusCd: "pending",
    requesterUserId: memberOne?.userId || "user01",
    requesterUserNm: memberOne?.userNm || "사용자",
    crtDt: seededAt,
    lastChgDt: seededAt,
  };

  const companySettings: CompanySettings = {
    companyNm: "StarWorks",
    senderNm: admin?.userNm || "관리자",
    senderEmail: "contracts@starworks.local",
    senderTelno: admin?.userTelno || "02-0000-0000",
    updatedAt: seededAt,
  };

  const batch = db.batch();
  templates.forEach((template) => {
    batch.set(db.collection(CONTRACT_TEMPLATES).doc(template.templateId), template);
  });
  [sampleContract, completedContract].forEach((contract) => {
    batch.set(db.collection(CONTRACTS).doc(contract.contractId), contract);
    contract.signers.forEach((signer) => {
      batch.set(db.collection(CONTRACT_PUBLIC).doc(signer.token), {
        contractId: contract.contractId,
        signerId: signer.signerId,
        token: signer.token,
      });
    });
  });
  batch.set(db.collection(CONTRACT_REQUESTS).doc(requestItem.requestId), requestItem);
  batch.set(db.collection(CONTRACT_SETTINGS).doc("main"), companySettings);
  batch.set(bootstrapRef, { seedVersion: 1, seededAt });
  await batch.commit();
}

async function listTemplatesData() {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACT_TEMPLATES).get();
  return snapshot.docs
    .map((doc) => normalizeTemplate(doc.data()))
    .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
}

async function listContractsData() {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACTS).get();
  return snapshot.docs
    .map((doc) => normalizeContract(doc.data()))
    .sort((left, right) => right.lastChgDt.localeCompare(left.lastChgDt));
}

async function listTemplateRequestsData() {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACT_REQUESTS).get();
  return snapshot.docs
    .map((doc) => normalizeTemplateRequest(doc.data()))
    .sort((left, right) => right.lastChgDt.localeCompare(left.lastChgDt));
}

async function getCompanySettingsData() {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACT_SETTINGS).doc("main").get();
  return normalizeCompanySettings(snapshot.data() || {});
}

async function getTemplateOrThrow(templateId: string) {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACT_TEMPLATES).doc(String(templateId)).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "템플릿을 찾을 수 없습니다.");
  }
  return normalizeTemplate(snapshot.data());
}

async function getContractOrThrow(contractId: string) {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACTS).doc(String(contractId)).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "계약을 찾을 수 없습니다.");
  }
  return normalizeContract(snapshot.data());
}

async function getTemplateRequestOrThrow(requestId: string) {
  await ensureContractSeedData();
  const snapshot = await db.collection(CONTRACT_REQUESTS).doc(String(requestId)).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "템플릿 요청을 찾을 수 없습니다.");
  }
  return normalizeTemplateRequest(snapshot.data());
}

async function buildContractRecord(payload: JsonMap, currentUserId: string, currentUserNm: string) {
  const templateId = String(payload.templateId || "");
  const template = await getTemplateOrThrow(templateId);
  const version = ensureTemplateVersion(template, template.publishedVersionId || template.currentVersionId);
  const signersPayload = Array.isArray(payload.signers) ? payload.signers : [];
  if (signersPayload.length === 0) {
    throw new HttpsError("invalid-argument", "signers are required.");
  }

  const signers = signersPayload.map((entry, index) => {
    const signer = asObject(entry);
    return normalizeSigner({
      signerId: makeId("SIGNER"),
      signerOrder: index + 1,
      signerNm: trimmed(signer.signerNm),
      signerEmail: trimmed(signer.signerEmail),
      signerTelno: trimmed(signer.signerTelno),
      claimedNm: trimmed(signer.signerNm),
      claimedEmail: trimmed(signer.signerEmail),
      claimedTelno: trimmed(signer.signerTelno),
      statusCd: "pending",
      submittedDt: "",
      token: makeId("PUBLIC"),
    }, index);
  });

  const contract: ContractRecord = {
    contractId: makeId("CTR"),
    contractRef: makeContractRef(),
    templateId: template.templateId,
    templateNm: template.templateNm,
    contractTitle: trimmed(payload.contractTitle) || `${template.templateNm} 계약`,
    contractMessage: trimmed(payload.contractMessage),
    sendTypeCd: trimmed(payload.sendTypeCd) || "remote",
    signingFlowCd: trimmed(payload.signingFlowCd) || "parallel",
    statusCd: "ready",
    expiresDt: trimmed(payload.expiresDt) || "",
    sentDt: "",
    lastChgDt: nowIso(),
    crtDt: nowIso(),
    creatorUserId: currentUserId,
    creatorUserNm: currentUserNm,
    currentSignOrder: 1,
    totalSignerCount: signers.length,
    signedCount: 0,
    signers,
    fieldMap: {},
    templateSchema: version?.schema || {},
    templateLayout: version?.layout || {},
  };

  return contract;
}

export const contractGetDashboard = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const [contracts, templates, requests] = await Promise.all([
    listContractsData(),
    listTemplatesData(),
    listTemplateRequestsData(),
  ]);

  return {
    counts: {
      active: contracts.filter((item) => decodeSection("active").has(item.statusCd)).length,
      completed: contracts.filter((item) => item.statusCd === "completed").length,
      templates: templates.length,
      requests: requests.filter((item) => item.statusCd === "pending").length,
    },
    recentContracts: contracts.slice(0, 5).map((item) => summarizeContract(item)),
  };
});

export const contractGetList = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const section = trimmed(asObject(request.data).section) || "active";
  const allowedStatuses = decodeSection(section);
  const contracts = await listContractsData();
  return {
    items: contracts.filter((item) => allowedStatuses.has(item.statusCd)).map((item) => summarizeContract(item)),
  };
});

export const contractGetDetail = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const contractId = trimmed(asObject(request.data).contractId);
  return getContractOrThrow(contractId);
});

export const contractCreate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const contract = await buildContractRecord(asObject(request.data), currentUser.userId, currentUser.userNm);
  await db.collection(CONTRACTS).doc(contract.contractId).set(contract);
  await syncPublicTokens(contract);
  return { contractId: contract.contractId };
});

export const contractSend = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const contractId = trimmed(asObject(request.data).contractId);
  const contract = await getContractOrThrow(contractId);
  contract.statusCd = contract.signedCount > 0 ? "in_progress" : "sent";
  contract.sentDt = contract.sentDt || nowIso();
  contract.lastChgDt = nowIso();
  await db.collection(CONTRACTS).doc(contract.contractId).set(contract, { merge: true });
  await syncPublicTokens(contract);
  return { contractId, statusCd: contract.statusCd };
});

export const contractCancel = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const contractId = trimmed(data.contractId);
  const reason = trimmed(data.reason);
  const contract = await getContractOrThrow(contractId);
  contract.statusCd = "cancelled";
  contract.cancelReason = reason;
  contract.lastChgDt = nowIso();
  await db.collection(CONTRACTS).doc(contract.contractId).set(contract, { merge: true });
  return { contractId, statusCd: contract.statusCd };
});

export const contractRemind = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const contractId = trimmed(asObject(request.data).contractId);
  const contract = await getContractOrThrow(contractId);
  contract.lastChgDt = nowIso();
  await db.collection(CONTRACTS).doc(contract.contractId).set(contract, { merge: true });
  return { contractId, remindedAt: contract.lastChgDt };
});

export const contractGetLinks = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const contractId = trimmed(asObject(request.data).contractId);
  const contract = await getContractOrThrow(contractId);
  return contract.signers.map((signer) => ({
    signerId: signer.signerId,
    signerNm: signer.claimedNm || signer.signerNm,
    publicUrl: `/contract/sign/${signer.token}`,
    statusCd: signer.statusCd,
  }));
});

export const contractCreateBatch = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const rows = Array.isArray(payload.rows) ? payload.rows : [];
  const batchId = makeId("BATCH");
  const contractIds: string[] = [];

  for (const row of rows) {
    const item = asObject(row);
    const contract = await buildContractRecord({
      templateId: payload.templateId,
      contractTitle: trimmed(item.contractTitle) || trimmed(payload.contractTitle),
      contractMessage: trimmed(payload.contractMessage),
      sendTypeCd: "remote",
      signingFlowCd: "parallel",
      signers: [{
        signerNm: trimmed(item.signerNm) || trimmed(item.recipientName),
        signerEmail: trimmed(item.signerEmail) || trimmed(item.recipientEmail),
        signerTelno: trimmed(item.signerTelno) || trimmed(item.recipientTelno),
      }],
    }, currentUser.userId, currentUser.userNm);
    await db.collection(CONTRACTS).doc(contract.contractId).set(contract);
    await syncPublicTokens(contract);
    contractIds.push(contract.contractId);
  }

  await db.collection(CONTRACT_BATCHES).doc(batchId).set({
    batchId,
    batchTitle: trimmed(payload.batchTitle) || "일괄 발송",
    templateId: String(payload.templateId || ""),
    contractIds,
    creatorUserId: currentUser.userId,
    creatorUserNm: currentUser.userNm,
    createdAt: nowIso(),
  });

  return { batchId, contractIds };
});

export const contractGetBatchDetail = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const batchId = trimmed(asObject(request.data).batchId);
  const snapshot = await db.collection(CONTRACT_BATCHES).doc(batchId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "배치 작업을 찾을 수 없습니다.");
  }
  return asObject(snapshot.data());
});

export const contractGetTemplates = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const templates = await listTemplatesData();
  return templates.map((item) => summarizeTemplate(item));
});

export const contractGetTemplateDetail = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const templateId = trimmed(asObject(request.data).templateId);
  const template = await getTemplateOrThrow(templateId);
  return {
    ...summarizeTemplate(template),
    currentVersion: template.versions.find((version) => version.templateVersionId === template.currentVersionId) || template.versions[0] || null,
  };
});

export const contractCreateTemplate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const payload = asObject(data.payload || data);
  const files = normalizeAttachmentsPayload(data);
  const templateId = String(Date.now());
  const version: TemplateVersion = {
    templateVersionId: makeId("TMPL_VER"),
    versionNo: 1,
    versionLabel: trimmed(payload.versionLabel) || "v1.0",
    versionNote: trimmed(payload.versionNote),
    schema: asObject(payload.schema),
    layout: asObject(payload.layout),
    sourceFile: files.sourceFile,
    backgroundFile: files.backgroundFile,
    createdAt: nowIso(),
    createdBy: currentUser.userId,
  };
  const template: ContractTemplate = {
    templateId,
    templateNm: trimmed(payload.templateNm) || "새 템플릿",
    templateCode: trimmed(payload.templateCode),
    templateDesc: trimmed(payload.templateDesc),
    templateCategoryCd: trimmed(payload.templateCategoryCd) || "general",
    currentVersionId: version.templateVersionId,
    publishedVersionId: payload.publishNow ? version.templateVersionId : "",
    versions: [version],
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  await db.collection(CONTRACT_TEMPLATES).doc(template.templateId).set(template);
  return { templateId: template.templateId };
});

export const contractUpdateTemplate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const templateId = trimmed(data.templateId);
  const payload = asObject(data.payload || data);
  const files = normalizeAttachmentsPayload(data);
  const template = await getTemplateOrThrow(templateId);
  const requestedVersionId = trimmed(payload.templateVersionId);
  const existingVersion = template.versions.find((version) => version.templateVersionId === requestedVersionId) || null;
  const nextVersion: TemplateVersion = {
    templateVersionId: existingVersion?.templateVersionId || makeId("TMPL_VER"),
    versionNo: existingVersion?.versionNo || template.versions.length + 1,
    versionLabel: trimmed(payload.versionLabel) || existingVersion?.versionLabel || `v${template.versions.length + 1}.0`,
    versionNote: trimmed(payload.versionNote) || existingVersion?.versionNote || "",
    schema: Object.keys(asObject(payload.schema)).length > 0 ? asObject(payload.schema) : existingVersion?.schema || {},
    layout: Object.keys(asObject(payload.layout)).length > 0 ? asObject(payload.layout) : existingVersion?.layout || {},
    sourceFile: files.sourceFile || existingVersion?.sourceFile || null,
    backgroundFile: files.backgroundFile || existingVersion?.backgroundFile || null,
    createdAt: existingVersion?.createdAt || nowIso(),
    createdBy: existingVersion?.createdBy || currentUser.userId,
  };
  const versions = existingVersion
    ? template.versions.map((version) => version.templateVersionId === existingVersion.templateVersionId ? nextVersion : version)
    : [...template.versions, nextVersion];
  const nextTemplate: ContractTemplate = {
    ...template,
    templateNm: trimmed(payload.templateNm) || template.templateNm,
    templateCode: trimmed(payload.templateCode) || template.templateCode,
    templateDesc: trimmed(payload.templateDesc) || template.templateDesc,
    templateCategoryCd: trimmed(payload.templateCategoryCd) || template.templateCategoryCd,
    currentVersionId: nextVersion.templateVersionId,
    publishedVersionId: payload.publishNow ? nextVersion.templateVersionId : template.publishedVersionId,
    versions,
    updatedAt: nowIso(),
  };
  await db.collection(CONTRACT_TEMPLATES).doc(template.templateId).set(nextTemplate, { merge: true });
  return { templateId: nextTemplate.templateId };
});

export const contractPublishTemplate = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const templateId = trimmed(data.templateId);
  const templateVersionId = trimmed(data.templateVersionId);
  const template = await getTemplateOrThrow(templateId);
  const versionId = templateVersionId || template.currentVersionId || template.versions[0]?.templateVersionId;
  await db.collection(CONTRACT_TEMPLATES).doc(templateId).set({
    publishedVersionId: versionId,
    updatedAt: nowIso(),
  }, { merge: true });
  return { templateId, templateVersionId: versionId };
});

export const contractGetTemplateRequests = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const requests = await listTemplateRequestsData();
  return requests.map((item) => summarizeTemplateRequest(item));
});

export const contractCreateTemplateRequest = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const payload = asObject(data.payload || data);
  const files = normalizeAttachmentsPayload(data);
  const requestItem: TemplateRequest = {
    requestId: makeId("REQ"),
    requestTitle: trimmed(payload.requestTitle) || "템플릿 요청",
    requestNote: trimmed(payload.requestNote),
    statusCd: "pending",
    requesterUserId: currentUser.userId,
    requesterUserNm: currentUser.userNm,
    sourceFile: files.sourceFile,
    markedFile: files.markedFile,
    sealFile: files.sealFile,
    crtDt: nowIso(),
    lastChgDt: nowIso(),
  };
  await db.collection(CONTRACT_REQUESTS).doc(requestItem.requestId).set(requestItem);
  return { requestId: requestItem.requestId };
});

export const contractApproveTemplateRequest = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const requestId = trimmed(asObject(request.data).requestId);
  const requestItem = await getTemplateRequestOrThrow(requestId);
  requestItem.statusCd = "approved";
  requestItem.lastChgDt = nowIso();
  await db.collection(CONTRACT_REQUESTS).doc(requestId).set(requestItem, { merge: true });
  return { requestId, statusCd: requestItem.statusCd };
});

export const contractRejectTemplateRequest = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const requestId = trimmed(asObject(request.data).requestId);
  const requestItem = await getTemplateRequestOrThrow(requestId);
  requestItem.statusCd = "rejected";
  requestItem.lastChgDt = nowIso();
  await db.collection(CONTRACT_REQUESTS).doc(requestId).set(requestItem, { merge: true });
  return { requestId, statusCd: requestItem.statusCd };
});

export const contractGetCompanySettings = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return getCompanySettingsData();
});

export const contractUpdateCompanySettings = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const payload = asObject(data.payload || data);
  const files = normalizeAttachmentsPayload(data);
  const current = await getCompanySettingsData();
  const nextSettings: CompanySettings = {
    companyNm: trimmed(payload.companyNm) || current.companyNm,
    senderNm: trimmed(payload.senderNm) || current.senderNm,
    senderEmail: trimmed(payload.senderEmail) || current.senderEmail,
    senderTelno: trimmed(payload.senderTelno) || current.senderTelno,
    sealFile: files.sealFile || current.sealFile || null,
    updatedAt: nowIso(),
  };
  await db.collection(CONTRACT_SETTINGS).doc("main").set(nextSettings, { merge: true });
  return nextSettings;
});

export const contractPublicDetail = onCall(async (request) => {
  await ensureContractSeedData();
  const token = trimmed(asObject(request.data).token);
  const tokenSnapshot = await db.collection(CONTRACT_PUBLIC).doc(token).get();
  if (!tokenSnapshot.exists) {
    throw new HttpsError("not-found", "유효하지 않은 서명 링크입니다.");
  }
  const tokenData = asObject(tokenSnapshot.data());
  const contract = await getContractOrThrow(trimmed(tokenData.contractId));
  const signer = contract.signers.find((entry) => entry.signerId === trimmed(tokenData.signerId) || entry.token === token);
  if (!signer) {
    throw new HttpsError("not-found", "서명 정보를 찾을 수 없습니다.");
  }
  const status = signerCanSubmit(contract, signer);
  return {
    contract: {
      contractId: contract.contractId,
      contractRef: contract.contractRef,
      contractTitle: contract.contractTitle,
      contractMessage: contract.contractMessage,
      statusCd: contract.statusCd,
      currentSignOrder: contract.currentSignOrder,
      templateSchema: contract.templateSchema,
      templateLayout: contract.templateLayout,
      fieldMap: contract.fieldMap,
    },
    signer,
    invitation: {
      token,
      expiresDt: contract.expiresDt,
    },
    canSubmit: status.canSubmit,
    blockedReason: status.blockedReason,
    downloadReady: contract.statusCd === "completed",
  };
});

export const contractPublicClaim = onCall(async (request) => {
  await ensureContractSeedData();
  const data = asObject(request.data);
  const token = trimmed(data.token);
  const tokenSnapshot = await db.collection(CONTRACT_PUBLIC).doc(token).get();
  if (!tokenSnapshot.exists) {
    throw new HttpsError("not-found", "유효하지 않은 서명 링크입니다.");
  }
  const tokenData = asObject(tokenSnapshot.data());
  const contract = await getContractOrThrow(trimmed(tokenData.contractId));
  contract.signers = contract.signers.map((signer) => signer.token === token ? {
    ...signer,
    claimedNm: trimmed(data.signerNm) || signer.claimedNm || signer.signerNm,
    claimedEmail: trimmed(data.signerEmail) || signer.claimedEmail || signer.signerEmail,
    claimedTelno: trimmed(data.signerTelno) || signer.claimedTelno || signer.signerTelno,
  } : signer);
  contract.lastChgDt = nowIso();
  await db.collection(CONTRACTS).doc(contract.contractId).set(contract, { merge: true });
  return { success: true };
});

export const contractPublicSubmit = onCall(async (request) => {
  await ensureContractSeedData();
  const data = asObject(request.data);
  const token = trimmed(data.token);
  const tokenSnapshot = await db.collection(CONTRACT_PUBLIC).doc(token).get();
  if (!tokenSnapshot.exists) {
    throw new HttpsError("not-found", "유효하지 않은 서명 링크입니다.");
  }
  const tokenData = asObject(tokenSnapshot.data());
  const contract = await getContractOrThrow(trimmed(tokenData.contractId));
  const signer = contract.signers.find((entry) => entry.token === token);
  if (!signer) {
    throw new HttpsError("not-found", "서명 정보를 찾을 수 없습니다.");
  }
  const status = signerCanSubmit(contract, signer);
  if (!status.canSubmit) {
    throw new HttpsError("failed-precondition", status.blockedReason);
  }

  const nextSigners = contract.signers.map((entry) => entry.token === token ? {
    ...entry,
    claimedNm: trimmed(data.signerNm) || entry.claimedNm || entry.signerNm,
    claimedEmail: trimmed(data.signerEmail) || entry.claimedEmail || entry.signerEmail,
    claimedTelno: trimmed(data.signerTelno) || entry.claimedTelno || entry.signerTelno,
    statusCd: "completed",
    submittedDt: nowIso(),
    signatureData: trimmed(data.signatureData),
    initialData: trimmed(data.initialData),
  } : entry);
  const nextStatus = nextContractStatusAfterSubmit(contract, nextSigners);
  const nextContract: ContractRecord = {
    ...contract,
    signers: nextSigners,
    fieldMap: {
      ...contract.fieldMap,
      ...asObject(data.fieldValues),
    },
    statusCd: nextStatus.statusCd,
    currentSignOrder: nextStatus.currentSignOrder,
    signedCount: nextStatus.signedCount,
    lastChgDt: nowIso(),
  };
  await db.collection(CONTRACTS).doc(contract.contractId).set(nextContract, { merge: true });
  return { success: true, statusCd: nextContract.statusCd };
});

export const contractPublicDownload = onCall(async (request) => {
  await ensureContractSeedData();
  const token = trimmed(asObject(request.data).token);
  const tokenSnapshot = await db.collection(CONTRACT_PUBLIC).doc(token).get();
  if (!tokenSnapshot.exists) {
    throw new HttpsError("not-found", "유효하지 않은 서명 링크입니다.");
  }
  const tokenData = asObject(tokenSnapshot.data());
  const contract = await getContractOrThrow(trimmed(tokenData.contractId));
  const pdfBase64 = buildSimplePdfBase64([
    `Contract Ref: ${contract.contractRef}`,
    `Title: ${contract.contractTitle}`,
    `Status: ${contract.statusCd}`,
    `Template: ${contract.templateNm}`,
    `Signed: ${contract.signedCount}/${contract.totalSignerCount}`,
    ...Object.entries(contract.fieldMap).slice(0, 8).map(([key, value]) => `${key}: ${String(value ?? "")}`),
  ]);
  return {
    fileName: `${contract.contractRef || contract.contractId}.pdf`,
    mimeType: "application/pdf",
    contentBase64: pdfBase64,
  };
});
