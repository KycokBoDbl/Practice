export const SPACE_TYPE_LABELS = {
  MEETING_ROOM: "Переговорная",
  CONFERENCE_HALL: "Конференц-зал",
  CLASSROOM: "Учебный класс",
  LOFT: "Лофт",
  SHOWROOM: "Шоурум",
} as const;

export type KnownSpaceType = keyof typeof SPACE_TYPE_LABELS;
export type SpaceType = KnownSpaceType | (string & {});

export function isKnownSpaceType(spaceType: SpaceType): spaceType is KnownSpaceType {
  return Object.prototype.hasOwnProperty.call(SPACE_TYPE_LABELS, spaceType);
}

export function getSpaceTypeLabel(spaceType: SpaceType) {
  return isKnownSpaceType(spaceType) ? SPACE_TYPE_LABELS[spaceType] : spaceType;
}
