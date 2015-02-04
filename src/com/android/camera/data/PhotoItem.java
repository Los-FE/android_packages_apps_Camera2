/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.android.camera.Storage;
import com.android.camera.data.FilmstripItemAttributes.Attributes;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.google.common.base.Optional;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Backing data for a single photo displayed in the filmstrip.
 */
public class PhotoItem extends FilmstripItemBase<FilmstripItemData> {
    private static final Log.Tag TAG = new Log.Tag("PhotoItem");
    private static final int MAX_PEEK_BITMAP_PIXELS = 1600000; // 1.6 * 4 MBs.

    private static final FilmstripItemAttributes PHOTO_ITEM_ATTRIBUTES =
          new FilmstripItemAttributes.Builder()
              .with(Attributes.CAN_SHARE)
              .with(Attributes.CAN_EDIT)
              .with(Attributes.CAN_DELETE)
              .with(Attributes.CAN_SWIPE_AWAY)
              .with(Attributes.CAN_SWIPE_IN_FULL_SCREEN)
              .with(Attributes.CAN_ZOOM_IN_PLACE)
              .with(Attributes.HAS_DETAILED_CAPTURE_INFO)
              .with(Attributes.IS_IMAGE)
              .build();

    private final PhotoItemFactory mPhotoItemFactory;

    private Bitmap mSessionPlaceholderBitmap;

    public PhotoItem(Context context, FilmstripItemData data, PhotoItemFactory photoItemFactory) {
        super(context, data, PHOTO_ITEM_ATTRIBUTES);
        mPhotoItemFactory = photoItemFactory;
    }

    /**
     * A bitmap that if present, is a high resolution bitmap from a temporary
     * session, that should be used as a placeholder in place of placeholder/
     * thumbnail loading.
     *
     * @param sessionPlaceholderBitmap a Bitmap to set as a placeholder
     */
    public void setSessionPlaceholderBitmap(Bitmap sessionPlaceholderBitmap) {
        mSessionPlaceholderBitmap = sessionPlaceholderBitmap;
    }

    @Override
    public String toString() {
        return "PhotoItem: " + mData.toString();
    }

    @Override
    public boolean delete() {
        ContentResolver cr = mContext.getContentResolver();
        cr.delete(PhotoDataQuery.CONTENT_URI,
              MediaStore.Images.ImageColumns._ID + "=" + mData.getContentId(), null);
        return super.delete();
    }

    @Override
    public Optional<MediaDetails> getMediaDetails() {
        Optional<MediaDetails> optionalDetails = super.getMediaDetails();
        if (optionalDetails.isPresent()) {
            MediaDetails mediaDetails = optionalDetails.get();
            MediaDetails.extractExifInfo(mediaDetails, mData.getFilePath());
            mediaDetails.addDetail(MediaDetails.INDEX_ORIENTATION, mData.getOrientation());
        }
        return optionalDetails;
    }

    @Override
    public FilmstripItem refresh() {
        // TODO: Consider simply replacing the data inline
        return mPhotoItemFactory.get(mData.getUri());
    }

    @Override
    public View getView(Optional<View> optionalView, int viewWidthPx, int viewHeightPx,
          LocalFilmstripDataAdapter adapter, boolean isInProgress,
          VideoClickedCallback videoClickedCallback) {
        ImageView imageView;

        if (optionalView.isPresent()) {
            imageView = (ImageView) optionalView.get();
        } else {
            imageView = (ImageView) LayoutInflater.from(mContext)
                  .inflate(R.layout.filmstrip_image, null);
            imageView.setTag(R.id.mediadata_tag_viewtype, getItemViewType().ordinal());
        }

        fillImageView(imageView);

        return imageView;
    }

    protected void fillImageView(final ImageView imageView) {
        Uri uri = mData.getUri();

        glideTinyThumb(uri)
            .placeholder(DEFAULT_PLACEHOLDER_RESOURCE)
            .dontAnimate()
            .into(imageView);

        // TODO consider having metadata have a "get description" string
        // or some other way of selecting rendering details based on metadata.
        int stringId = R.string.photo_date_content_description;
        if (getMetadata().isPanorama() ||
              getMetadata().isPanorama360()) {
            stringId = R.string.panorama_date_content_description;
        } else if (getMetadata().isUsePanoramaViewer()) {
            // assume it's a PhotoSphere
            stringId = R.string.photosphere_date_content_description;
        } else if (this.getMetadata().isHasRgbzData()) {
            stringId = R.string.refocus_date_content_description;
        }

        imageView.setContentDescription(mContext.getResources().getString(
              stringId,
              mDateFormatter.format(mData.getLastModifiedDate())));
    }

    @Override
    public void recycle(View view) {
        Glide.clear(view);
        mSessionPlaceholderBitmap = null;
    }

    @Override
    public FilmstripItemType getItemViewType() {
        return FilmstripItemType.PHOTO;
    }

    @Override
    public void loadFullImage(int thumbWidth, int thumbHeight, View v) {
        Uri uri = mData.getUri();
        Size size = mData.getDimensions();

        BitmapRequestBuilder<Uri, Bitmap> builder =
                glideFullResBitmap(uri, size.getWidth(), size.getHeight());

        if (mSessionPlaceholderBitmap != null) {
            builder.placeholder(new BitmapDrawable(mContext.getResources(),
                    mSessionPlaceholderBitmap));
        } else {
            builder
                    .thumbnail(glideTinyThumb(uri))
                    .placeholder(DEFAULT_PLACEHOLDER_RESOURCE);
        }

        builder
              .dontAnimate()
              .into((ImageView) v);
    }

    @Override
    public Optional<Bitmap> generateThumbnail(int boundingWidthPx, int boundingHeightPx) {
        FilmstripItemData data = getData();
        final Bitmap bitmap;

        if (getAttributes().isRendering()) {
            bitmap = Storage.getPlacerHolderForSession(data.getUri());
        } else {

            FileInputStream stream;

            try {
                stream = new FileInputStream(data.getFilePath());
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found:" + data.getFilePath());
                return Optional.absent();
            }
            int width = data.getDimensions().getWidth();
            int height = data.getDimensions().getHeight();
            int orientation = data.getOrientation();

            Point dim = CameraUtil.resizeToFill(
                  width,
                  height,
                  orientation,
                  boundingWidthPx,
                  boundingHeightPx);

            // If the orientation is not vertical
            if (orientation % 180 != 0) {
                int dummy = dim.x;
                dim.x = dim.y;
                dim.y = dummy;
            }

            bitmap = FilmstripItemUtils
                  .loadImageThumbnailFromStream(
                        stream,
                        data.getDimensions().getWidth(),
                        data.getDimensions().getHeight(),
                        (int) (dim.x * 0.7f), (int) (dim.y * 0.7),
                        data.getOrientation(), MAX_PEEK_BITMAP_PIXELS);
        }

        return Optional.fromNullable(bitmap);
    }
}