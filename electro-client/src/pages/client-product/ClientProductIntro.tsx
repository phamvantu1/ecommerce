import {
  ActionIcon,
  Anchor,
  Badge,
  Box,
  Breadcrumbs,
  Button,
  Card,
  Grid,
  Group,
  Image,
  NumberInput,
  NumberInputHandlers,
  SimpleGrid,
  Stack,
  Text,
  UnstyledButton,
  useMantineTheme
} from '@mantine/core';
import { Link } from 'react-router-dom';
import MiscUtils from 'utils/MiscUtils';
import { ClientCarousel, ReviewStarGroup } from 'components';
import { BellPlus, Heart, PhotoOff, ShoppingCart } from 'tabler-icons-react';
import React, { useRef, useState } from 'react';
import { ClientPreorderRequest, ClientProductResponse, ClientWishRequest } from 'types';
import useCreateWishApi from 'hooks/use-create-wish-api';
import NotifyUtils from 'utils/NotifyUtils';
import useAuthStore from 'stores/use-auth-store';
import useCreatePreorderApi from 'hooks/use-create-preorder-api';

interface ClientProductIntroProps {
  product: ClientProductResponse;
}

function ClientProductIntro({ product }: ClientProductIntroProps) {
  const theme = useMantineTheme();

  const [selectedVariantIndex, setSelectedVariantIndex] = useState(0);

  const { user } = useAuthStore();

  const createWishApi = useCreateWishApi();
  const createPreorderApi = useCreatePreorderApi();

  const handleCreateWishButton = () => {
    if (!user) {
      NotifyUtils.simple('Vui lòng đăng nhập để sử dụng chức năng');
    } else {
      const clientWishRequest: ClientWishRequest = {
        userId: user.id,
        productId: product.productId,
      };
      createWishApi.mutate(clientWishRequest);
    }
  };

  const handleCreatePreorderButton = () => {
    if (!user) {
      NotifyUtils.simple('Vui lòng đăng nhập để sử dụng chức năng');
    } else {
      const clientPreorderRequest: ClientPreorderRequest = {
        userId: user.id,
        productId: product.productId,
        status: 1,
      };
      createPreorderApi.mutate(clientPreorderRequest);
    }
  };

  return (
    <Card radius="md" shadow="sm" p="lg">
      <Stack>
        <Breadcrumbs>
          <Anchor component={Link} to="/">
            Trang chủ
          </Anchor>
          {product.productCategory && MiscUtils.makeCategoryBreadcrumbs(product.productCategory).map(c => (
            <Anchor key={c.categorySlug} component={Link} to={'/category/' + c.categorySlug}>
              {c.categoryName}
            </Anchor>
          ))}
          <Text color="dimmed">
            {product.productName}
          </Text>
        </Breadcrumbs>

        <Grid gutter="lg">
          <Grid.Col md={6}>
            {product.productImages.length > 0
              ? (
                <ClientCarousel>
                  {product.productImages.map(image => (
                    <Image
                      key={image.id}
                      radius="md"
                      src={image.path}
                      styles={{ image: { aspectRatio: '1 / 1' } }}
                      withPlaceholder
                    />
                  ))}
                </ClientCarousel>
              )
              : (
                <Box
                  sx={{
                    borderRadius: theme.radius.md,
                    width: '100%',
                    height: 'auto',
                    aspectRatio: '1 / 1',
                    border: `2px dotted ${theme.colors.gray[5]}`,
                  }}
                >
                  <Stack align="center" justify="center" sx={{ height: '100%' }}>
                    <PhotoOff size={100} strokeWidth={1}/>
                    <Text>Không có hình cho sản phẩm này</Text>
                  </Stack>
                </Box>
              )}
          </Grid.Col>
          <Grid.Col md={6}>
            <Stack spacing="lg">
              <Stack spacing={2} sx={{ alignItems: 'start' }}>
                {!product.productSaleable && <Badge color="red" variant="filled" mb={5}>Hết hàng</Badge>}
                {product.productBrand && (
                  <Group spacing={5}>
                    <Text size="sm">Thương hiệu:</Text>
                    <Anchor component={Link} to={'/brand/' + product.productBrand.brandId} size="sm">
                      {product.productBrand.brandName}
                    </Anchor>
                  </Group>
                )}
                <Text sx={{ fontSize: 26 }} weight={500}>
                  {product.productName}
                </Text>
                <Group mt={7.5} spacing="lg">
                  <Group spacing="xs">
                    <ReviewStarGroup ratingScore={product.productAverageRatingScore}/>
                    <Text size="sm">{product.productCountReviews} đánh giá</Text>
                  </Group>
                  <Group spacing={5}>
                    <ShoppingCart size={18} strokeWidth={1.5} color={theme.colors.teal[7]}/>
                    {/* TODO */}
                    <Text size="sm" color="teal">120 đã mua</Text>
                  </Group>
                </Group>
              </Stack>

              {product.productShortDescription && <Text color="dimmed">{product.productShortDescription}</Text>}

              <Box
                sx={{
                  backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[5] : theme.colors.gray[0],
                  borderRadius: theme.radius.md,
                  padding: '16px 20px',
                }}
              >
                {/* TODO */}
                <Group>
                  <Text sx={{ fontSize: 24 }} weight={700} color="pink">
                    {MiscUtils.formatPrice(product.productVariants[selectedVariantIndex]?.variantPrice)} ₫
                  </Text>
                  <Text sx={{ textDecoration: 'line-through' }}>
                    4.000.000 ₫
                  </Text>
                  <Badge color="pink" size="lg" variant="filled">
                    -50%
                  </Badge>
                </Group>
              </Box>

              <Stack spacing="xs">
                <Text weight={500}>Phiên bản</Text>
                {product.productVariants.length > 0
                  ? product.productVariants.some(variant => variant.variantProperties)
                    ? (
                      <Group>
                        {product.productVariants.map((variant, index) => (
                          <UnstyledButton
                            key={variant.variantId}
                            sx={{
                              borderRadius: theme.radius.md,
                              padding: '7.5px 15px',
                              border: `2px solid ${theme.colorScheme === 'dark'
                                ? (index === selectedVariantIndex ? theme.colors.blue[9] : theme.colors.dark[3])
                                : (index === selectedVariantIndex ? theme.colors.blue[4] : theme.colors.gray[2])}`,
                              backgroundColor: index === selectedVariantIndex
                                ? (theme.colorScheme === 'dark'
                                  ? theme.fn.rgba(theme.colors.blue[9], 0.25)
                                  : theme.colors.blue[0])
                                : 'unset',
                            }}
                            onClick={() => setSelectedVariantIndex(index)}
                          >
                            <SimpleGrid cols={2} spacing={5}>
                              {variant.variantProperties?.content.map(property => (
                                <React.Fragment key={property.id}>
                                  <Text size="sm">{property.name}</Text>
                                  <Text
                                    size="sm"
                                    sx={{ textAlign: 'right', fontWeight: 500 }}
                                  >
                                    {property.value}
                                  </Text>
                                </React.Fragment>
                              ))}
                            </SimpleGrid>
                          </UnstyledButton>
                        ))}
                      </Group>
                    )
                    : <Text color="dimmed" size="sm">Sản phẩm chỉ có duy nhất một phiên bản mặc định</Text>
                  : <Text color="dimmed" size="sm">Không có phiên bản nào</Text>}
              </Stack>

              {product.productSaleable && (
                <Stack spacing="xs">
                  <Text weight={500}>Số lượng</Text>
                  <ClientProductQuantityInput/>
                </Stack>
              )}

              <Group mt={theme.spacing.md}>
                {!product.productSaleable
                  ? (
                    <Button
                      radius="md"
                      size="lg"
                      color="teal"
                      leftIcon={<BellPlus/>}
                      onClick={handleCreatePreorderButton}
                    >
                      Đặt trước
                    </Button>
                  )
                  : (
                    <Button
                      radius="md"
                      size="lg"
                      color="pink"
                      leftIcon={<ShoppingCart/>}
                    >
                      Chọn mua
                    </Button>
                  )}
                <Button
                  radius="md"
                  size="lg"
                  color="pink"
                  variant="outline"
                  leftIcon={<Heart/>}
                  onClick={handleCreateWishButton}
                >
                  Yêu thích
                </Button>
              </Group>
            </Stack>
          </Grid.Col>
        </Grid>
      </Stack>
    </Card>
  );
}

function ClientProductQuantityInput() {
  const [value, setValue] = useState(1);
  const handlers = useRef<NumberInputHandlers>();

  return (
    <Group spacing={5}>
      <ActionIcon size={36} variant="default" onClick={() => handlers.current?.decrement()}>
        –
      </ActionIcon>

      <NumberInput
        hideControls
        value={value}
        onChange={(val) => setValue(val || 1)}
        handlersRef={handlers}
        max={100}
        min={1}
        styles={{ input: { width: 54, textAlign: 'center' } }}
      />

      <ActionIcon size={36} variant="default" onClick={() => handlers.current?.increment()}>
        +
      </ActionIcon>
    </Group>
  );
}

export default ClientProductIntro;
