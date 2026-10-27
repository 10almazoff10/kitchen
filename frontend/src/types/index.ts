export interface UserDto {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  avatarUrl?: string;
  adUsername?: string;
  legacyUsername?: string;
  isAdLinked?: boolean;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  tokenType: string;
  user: UserDto;
  requiresAdLink?: boolean;
}

export interface RestaurantResponse {
  id: number;
  name: string;
  websiteUrl?: string;
  createdAt?: string;
}

export interface OrderItemResponse {
  id: number;
  orderId: number;
  userId: number;
  userFullName: string;
  itemDescription: string;
  price: number;
  deliveryCost: number;
  totalCost: number;
  isAddedToRestaurantOrder?: boolean;
  addedToRestaurantOrder?: boolean;
  isPaid?: boolean;
  paid?: boolean;
  createdDate?: string;
  rating?: number | null;
}

export interface UserOrderSummaryDto {
  userId: number;
  userFullName: string;
  avatarUrl?: string;
  items: OrderItemResponse[];
  deliveryCost: number;
  totalAmount: number;
  allPaid: boolean;
  allAdded: boolean;
}

export interface OrderResponse {
  id: number;
  restaurant: RestaurantResponse;
  deadlineTime: string;
  isClosed?: boolean;
  closed?: boolean;
  isAcceptingOrders?: boolean;
  acceptingOrders?: boolean;
  paymentData?: string;
  comment?: string;
  createdBy: UserDto;
  totalAmount: number;
  itemsCount: number;
}

export interface OrderDetailResponse {
  order: OrderResponse;
  items?: OrderItemResponse[];
  userSummaries: UserOrderSummaryDto[];
  totalAmount: number;
  totalDeliveryCost: number;
}

export interface CreateOrderRequest {
  restaurantId?: number;
  newRestaurantName?: string;
  newRestaurantUrl?: string;
  deadlineTime: string;
  paymentData?: string;
  comment?: string;
}

export interface CloseOrderRequest {
  deliveryCost: number;
}

export interface AddOrderItemRequest {
  itemDescription: string;
  price: number;
}

export interface UpdateOrderItemRequest {
  itemDescription: string;
  price: number;
}

export interface RestaurantRequest {
  name: string;
  websiteUrl: string;
}

export interface RateItemRequest {
  rating: number;
}

export interface RatingLeaderboardItemDto {
  userId: number;
  username: string;
  fullName: string;
  email?: string;
  avatarUrl?: string;
  amount?: number | null;
  count?: number | null;
}

export interface UpdateProfileRequest {
  email?: string;
  fullName?: string;
}

export interface RatingLeaderboardResponse {
  topSpentThisMonth: RatingLeaderboardItemDto[];
  topOrdersThisMonth: RatingLeaderboardItemDto[];
  topSpentAllTime: RatingLeaderboardItemDto[];
  topOrdersAllTime: RatingLeaderboardItemDto[];
}

export interface UserStatisticResponse {
  totalSpentThisMonth: number;
  totalOrdersThisMonth: number;
  totalSpentAllTime: number;
  totalOrdersAllTime: number;
}

export interface NotPaidOrdersResponse {
  items: OrderItemResponse[];
  totalDebt: number;
  totalCount: number;
}

export interface ParsedMenuItemDto {
  name: string;
  price: number;
  description?: string;
  imageUrl?: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface DishSuggestionDto {
  itemDescription: string;
  price: number;
}

export interface PollOptionDto {
  restaurantId: number;
  restaurantName: string;
  restaurantUrl?: string;
  votesCount: number;
  percentage: number;
  voters: UserDto[];
  isMyVote: boolean;
}

export interface DailyPollResponse {
  id: number;
  pollDate: string;
  isActive: boolean;
  totalVotes: number;
  myVotedRestaurantId?: number | null;
  myVotedRestaurantIds?: number[];
  options: PollOptionDto[];
}
